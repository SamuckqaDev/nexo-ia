package com.nexoia.workspace.tool;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditOutcome;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.provider.dto.ToolExecutionEvidence;
import com.nexoia.provider.dto.ToolExecutionObserver;
import com.nexoia.provider.dto.ToolExecutionStarted;
import com.nexoia.provider.dto.ToolExecutionStatus;
import com.nexoia.provider.dto.WorkspaceToolScope;
import com.nexoia.workspace.dto.WorkspaceFileResponse;
import com.nexoia.workspace.dto.WorkspaceGitSummary;
import com.nexoia.workspace.dto.WorkspaceTreeResponse;
import com.nexoia.workspace.exception.WorkspaceAccessDeniedException;
import com.nexoia.workspace.exception.WorkspaceFileNotTextException;
import com.nexoia.workspace.exception.WorkspaceFileTooLargeException;
import com.nexoia.workspace.exception.WorkspaceInvalidPathException;
import com.nexoia.workspace.exception.WorkspaceUnavailableException;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.model.WorkspaceStatus;
import com.nexoia.workspace.service.WorkspaceAccessService;
import com.nexoia.workspace.service.WorkspaceContentPolicy;
import com.nexoia.workspace.service.WorkspaceGitReadService;
import com.nexoia.workspace.service.WorkspaceInspectionService;
import com.nexoia.workspace.service.WorkspacePathResolver;
import com.nexoia.workspace.service.WorkspaceSearchService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Builds governed request-scoped Spring AI tools for one attached Workspace. */
@Slf4j
@Component
public class WorkspaceReadToolFactory {

    public static final String LIST_FILES = "workspace_list_files";
    public static final String READ_FILE = "workspace_read_file";
    public static final String WRITE_FILE = "workspace_write_file";
    public static final String SEARCH = "workspace_search";
    public static final String GIT_STATUS = "workspace_git_status";
    public static final String GIT_DIFF = "workspace_git_diff";
    public static final String INSPECT_PROJECT = "workspace_inspect_project";
    public static final int MAX_CALLS = 12;
    private static final int DEFAULT_LIST_LIMIT = 100;
    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int MAX_QUERY_LENGTH = 1_000;
    private static final int MAX_WRITE_BYTES = 1_048_576;

    private final WorkspaceAccessService access;
    private final WorkspaceInspectionService inspection;
    private final WorkspaceSearchService search;
    private final WorkspaceGitReadService git;
    private final WorkspacePathResolver pathResolver;
    private final WorkspaceContentPolicy contentPolicy;
    private final AuditService audit;
    private final Clock clock;
    private final LocalWorkspaceToolGateway localGateway;

    @Autowired
    public WorkspaceReadToolFactory(
            WorkspaceAccessService access,
            WorkspaceInspectionService inspection,
            WorkspaceSearchService search,
            WorkspaceGitReadService git,
            WorkspacePathResolver pathResolver,
            WorkspaceContentPolicy contentPolicy,
            AuditService audit,
            Clock clock,
            LocalWorkspaceToolGateway localGateway) {
        this.access = access;
        this.inspection = inspection;
        this.search = search;
        this.git = git;
        this.pathResolver = pathResolver;
        this.contentPolicy = contentPolicy;
        this.audit = audit;
        this.clock = clock;
        this.localGateway = localGateway;
    }

    public WorkspaceReadToolFactory(
            WorkspaceAccessService access,
            WorkspaceInspectionService inspection,
            WorkspaceSearchService search,
            WorkspaceGitReadService git,
            WorkspacePathResolver pathResolver,
            WorkspaceContentPolicy contentPolicy,
            AuditService audit,
            Clock clock) {
        this(access, inspection, search, git, pathResolver, contentPolicy, audit, clock, null);
    }

    public WorkspaceReadToolSession open(
            WorkspaceToolScope scope,
            ToolExecutionObserver observer,
            BooleanSupplier cancelled) {
        List<ToolExecutionEvidence> evidence = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        Set<String> seenCalls = new HashSet<>();

        List<ToolCallback> callbacks = new ArrayList<>(List.of(
                FunctionToolCallback.builder(
                                LIST_FILES,
                                (WorkspaceListFilesInput input, ToolContext ignored) -> listFiles(
                                        scope, observer, cancelled, evidence, calls, seenCalls, input))
                        .description("List one directory in the attached workspace using relative paths only")
                        .inputType(WorkspaceListFilesInput.class)
                        .build(),
                FunctionToolCallback.builder(
                                READ_FILE,
                                (WorkspaceReadFileInput input, ToolContext ignored) -> readFile(
                                        scope, observer, cancelled, evidence, calls, seenCalls, input))
                        .description("Read a bounded UTF-8 excerpt from a non-sensitive workspace file")
                        .inputType(WorkspaceReadFileInput.class)
                        .build(),
                FunctionToolCallback.builder(
                                SEARCH,
                                (WorkspaceSearchInput input, ToolContext ignored) -> search(
                                        scope, observer, cancelled, evidence, calls, seenCalls, input))
                        .description("Search literal text across safe files in the attached workspace")
                        .inputType(WorkspaceSearchInput.class)
                        .build(),
                FunctionToolCallback.builder(
                                GIT_STATUS,
                                (WorkspaceProjectQueryInput input, ToolContext ignored) -> gitStatus(
                                        scope, observer, cancelled, evidence, calls, seenCalls, input))
                        .description("Inspect the current read-only Git status of the attached workspace")
                        .inputType(WorkspaceProjectQueryInput.class)
                        .build(),
                FunctionToolCallback.builder(
                                GIT_DIFF,
                                (WorkspaceGitDiffInput input, ToolContext ignored) -> gitDiff(
                                        scope, observer, cancelled, evidence, calls, seenCalls, input))
                        .description("Read a bounded Git diff for one explicit non-sensitive workspace file")
                        .inputType(WorkspaceGitDiffInput.class)
                        .build(),
                FunctionToolCallback.builder(
                                INSPECT_PROJECT,
                                (WorkspaceProjectQueryInput input, ToolContext ignored) -> inspectProject(
                                        scope, observer, cancelled, evidence, calls, seenCalls, input))
                        .description("Inspect the attached project stack, branch, and HEAD without changing it")
                        .inputType(WorkspaceProjectQueryInput.class)
                        .build()));
        if (scope.writeAuthorized()) {
            callbacks.add(FunctionToolCallback.builder(
                            WRITE_FILE,
                            (WorkspaceWriteFileInput input, ToolContext ignored) -> writeFile(
                                    scope, observer, cancelled, evidence, calls, seenCalls, input))
                    .description("Create one bounded UTF-8 Workspace file, or replace an existing file only "
                            + "with the current SHA-256 returned by workspace_read_file")
                    .inputType(WorkspaceWriteFileInput.class)
                    .build());
        }
        return new WorkspaceReadToolSession(callbacks, evidence);
    }

    private WorkspaceWriteFileResult writeFile(
            WorkspaceToolScope scope,
            ToolExecutionObserver observer,
            BooleanSupplier cancelled,
            List<ToolExecutionEvidence> evidence,
            AtomicInteger calls,
            Set<String> seenCalls,
            WorkspaceWriteFileInput input) {
        Call call = begin(scope, observer, WRITE_FILE, input, cancelled, calls, seenCalls);
        if (!call.allowed()
                || !scope.writeAuthorized()
                || input == null
                || input.path() == null
                || input.path().isBlank()
                || input.content() == null) {
            finish(scope, observer, evidence, call, ToolExecutionStatus.DENIED);
            return failedWrite(ToolExecutionStatus.DENIED,
                    "Workspace write was not authorized by this explicit user request.");
        }
        byte[] content = input.content().getBytes(StandardCharsets.UTF_8);
        if (content.length > MAX_WRITE_BYTES || input.content().indexOf('\0') >= 0) {
            finish(scope, observer, evidence, call, ToolExecutionStatus.DENIED);
            return failedWrite(ToolExecutionStatus.DENIED,
                    "Workspace writes are limited to bounded UTF-8 text.");
        }
        try {
            if (scope.localDevice()) {
                WorkspaceWriteFileResult result = localGateway.writeFile(scope, input);
                finish(scope, observer, evidence, call, result.status());
                return result;
            }
            Workspace workspace = writableWorkspace(scope);
            String relativePath = input.path().trim();
            contentPolicy.requireReadable(relativePath);
            Path candidate = pathResolver.resolveForCreate(workspace, relativePath);
            Path target = Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)
                    ? pathResolver.resolveExisting(workspace, relativePath)
                    : candidate;
            Path parent = target.getParent();
            if (parent == null || !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
                throw new WorkspaceInvalidPathException();
            }
            boolean created = !Files.exists(target, LinkOption.NOFOLLOW_LINKS);
            if (!created) {
                if (Files.isSymbolicLink(target) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                    throw new WorkspaceAccessDeniedException();
                }
                if (Files.size(target) > MAX_WRITE_BYTES) {
                    throw new WorkspaceFileTooLargeException();
                }
                String currentSha256 = digestBytes(Files.readAllBytes(target));
                if (input.expectedSha256() == null
                        || !currentSha256.equalsIgnoreCase(input.expectedSha256().trim())) {
                    finish(scope, observer, evidence, call, ToolExecutionStatus.DENIED);
                    return failedWrite(ToolExecutionStatus.DENIED,
                            "Existing files require the current SHA-256 from workspace_read_file.");
                }
            }
            Path temporary = Files.createTempFile(parent, ".nexo-write-", ".tmp");
            try {
                Files.write(temporary, content, StandardOpenOption.TRUNCATE_EXISTING);
                moveAtomically(temporary, target);
            } finally {
                Files.deleteIfExists(temporary);
            }
            String sha256 = digestBytes(content);
            finish(scope, observer, evidence, call, ToolExecutionStatus.COMPLETED);
            return new WorkspaceWriteFileResult(
                    ToolExecutionStatus.COMPLETED,
                    relativePath,
                    created,
                    content.length,
                    sha256,
                    created ? "Workspace file created successfully." : "Workspace file replaced successfully.");
        } catch (RuntimeException exception) {
            ToolExecutionStatus status = finishException(scope, observer, evidence, call, exception);
            return failedWrite(status, status == ToolExecutionStatus.DENIED
                    ? "Workspace write was denied by the path or content policy."
                    : "Workspace file could not be written.");
        } catch (IOException exception) {
            log.warn("[NEXO-BACK][WORKSPACE] Write tool failed workspaceId={} reason={}",
                    scope.workspaceId(), exception.getClass().getSimpleName());
            finish(scope, observer, evidence, call, ToolExecutionStatus.UNAVAILABLE);
            return failedWrite(ToolExecutionStatus.UNAVAILABLE, "Workspace file could not be written.");
        }
    }

    private WorkspaceListFilesResult listFiles(
            WorkspaceToolScope scope,
            ToolExecutionObserver observer,
            BooleanSupplier cancelled,
            List<ToolExecutionEvidence> evidence,
            AtomicInteger calls,
            Set<String> seenCalls,
            WorkspaceListFilesInput input) {
        Call call = begin(scope, observer, LIST_FILES, input, cancelled, calls, seenCalls);
        if (!call.allowed()) {
            finish(scope, observer, evidence, call, ToolExecutionStatus.DENIED);
            return new WorkspaceListFilesResult(
                    ToolExecutionStatus.DENIED, "", List.of(), List.of(), false, null,
                    "Workspace listing was denied by the request policy.");
        }
        try {
            if (scope.localDevice()) {
                WorkspaceListFilesResult result = localGateway.listFiles(scope, input);
                finish(scope, observer, evidence, call, result.status());
                return result;
            }
            Workspace workspace = workspace(scope);
            WorkspaceTreeResponse tree = inspection.tree(
                    workspace,
                    input == null ? null : input.path(),
                    input == null || input.limit() == null ? DEFAULT_LIST_LIMIT : input.limit(),
                    input == null ? null : input.cursor());
            finish(scope, observer, evidence, call, ToolExecutionStatus.COMPLETED);
            return new WorkspaceListFilesResult(
                    ToolExecutionStatus.COMPLETED, tree.path(), tree.entries(), tree.omissions(),
                    tree.truncated(), tree.nextCursor(), "Workspace directory listed successfully.");
        } catch (RuntimeException exception) {
            return failedList(scope, observer, evidence, call, exception);
        }
    }

    private WorkspaceReadFileResult readFile(
            WorkspaceToolScope scope,
            ToolExecutionObserver observer,
            BooleanSupplier cancelled,
            List<ToolExecutionEvidence> evidence,
            AtomicInteger calls,
            Set<String> seenCalls,
            WorkspaceReadFileInput input) {
        Call call = begin(scope, observer, READ_FILE, input, cancelled, calls, seenCalls);
        if (!call.allowed() || input == null || input.path() == null || input.path().isBlank()) {
            finish(scope, observer, evidence, call, ToolExecutionStatus.DENIED);
            return failedRead(ToolExecutionStatus.DENIED, "Workspace file read was denied by the request policy.");
        }
        try {
            if (scope.localDevice()) {
                WorkspaceReadFileResult result = localGateway.readFile(scope, input);
                finish(scope, observer, evidence, call, result.status());
                return result;
            }
            WorkspaceFileResponse file = inspection.file(
                    workspace(scope), input.path().trim(), input.startLine(), input.endLine());
            finish(scope, observer, evidence, call, ToolExecutionStatus.COMPLETED);
            return new WorkspaceReadFileResult(
                    ToolExecutionStatus.COMPLETED,
                    file.path(),
                    numbered(file.content(), file.startLine()),
                    file.startLine(),
                    file.endLine(),
                    file.totalLines(),
                    file.sha256(),
                    file.truncated(),
                    "Workspace file read successfully.");
        } catch (RuntimeException exception) {
            ToolExecutionStatus status = finishException(scope, observer, evidence, call, exception);
            return failedRead(status, status == ToolExecutionStatus.DENIED
                    ? "Workspace file read was denied by the content policy."
                    : "Workspace file could not be read safely.");
        }
    }

    private WorkspaceSearchResult search(
            WorkspaceToolScope scope,
            ToolExecutionObserver observer,
            BooleanSupplier cancelled,
            List<ToolExecutionEvidence> evidence,
            AtomicInteger calls,
            Set<String> seenCalls,
            WorkspaceSearchInput input) {
        Call call = begin(scope, observer, SEARCH, input, cancelled, calls, seenCalls);
        String query = input == null || input.query() == null ? "" : input.query().trim();
        if (!call.allowed() || query.isBlank() || query.length() > MAX_QUERY_LENGTH) {
            finish(scope, observer, evidence, call, ToolExecutionStatus.DENIED);
            return new WorkspaceSearchResult(
                    ToolExecutionStatus.DENIED, List.of(), false,
                    "Workspace search was denied by the request policy.");
        }
        try {
            if (scope.localDevice()) {
                WorkspaceSearchResult result = localGateway.search(scope, input);
                finish(scope, observer, evidence, call, result.status());
                return result;
            }
            int requested = input.limit() == null ? DEFAULT_SEARCH_LIMIT : input.limit();
            int limit = Math.max(1, Math.min(requested, 100));
            List<WorkspaceSearchMatch> found = search.search(
                    workspace(scope), query, input.path(), limit + 1);
            boolean truncated = found.size() > limit;
            List<WorkspaceSearchMatch> bounded = found.stream().limit(limit).toList();
            ToolExecutionStatus status = bounded.isEmpty()
                    ? ToolExecutionStatus.NO_RESULTS
                    : ToolExecutionStatus.FOUND;
            finish(scope, observer, evidence, call, status);
            return new WorkspaceSearchResult(
                    status, bounded, truncated,
                    bounded.isEmpty() ? "No matching workspace text was found." : "Workspace matches found.");
        } catch (RuntimeException exception) {
            ToolExecutionStatus status = finishException(scope, observer, evidence, call, exception);
            return new WorkspaceSearchResult(
                    status, List.of(), false,
                    status == ToolExecutionStatus.DENIED
                            ? "Workspace search was denied by the content policy."
                            : "Workspace search is unavailable.");
        }
    }

    private WorkspaceGitStatusResult gitStatus(
            WorkspaceToolScope scope,
            ToolExecutionObserver observer,
            BooleanSupplier cancelled,
            List<ToolExecutionEvidence> evidence,
            AtomicInteger calls,
            Set<String> seenCalls,
            WorkspaceProjectQueryInput input) {
        Call call = begin(scope, observer, GIT_STATUS, input, cancelled, calls, seenCalls);
        if (!call.allowed()) {
            finish(scope, observer, evidence, call, ToolExecutionStatus.DENIED);
            return new WorkspaceGitStatusResult(
                    ToolExecutionStatus.DENIED, null, null, List.of(), false,
                    "Git status was denied by the request policy.");
        }
        try {
            if (scope.localDevice()) {
                WorkspaceGitStatusResult result = localGateway.gitStatus(scope, input);
                finish(scope, observer, evidence, call, result.status());
                return result;
            }
            Workspace workspace = workspace(scope);
            String raw = git.status(workspace);
            Optional<WorkspaceGitSummary> summary = inspection.gitSummary(pathResolver.workspaceRoot(workspace));
            List<String> changed = safeChangedPaths(raw);
            finish(scope, observer, evidence, call, ToolExecutionStatus.COMPLETED);
            return new WorkspaceGitStatusResult(
                    ToolExecutionStatus.COMPLETED,
                    summary.map(WorkspaceGitSummary::branch).orElse(null),
                    summary.map(WorkspaceGitSummary::head).orElse(null),
                    changed,
                    false,
                    changed.isEmpty() ? "Git working tree is clean." : "Git working tree has changes.");
        } catch (RuntimeException exception) {
            ToolExecutionStatus status = finishException(scope, observer, evidence, call, exception);
            return new WorkspaceGitStatusResult(
                    status, null, null, List.of(), false,
                    "Git status is unavailable.");
        }
    }

    private WorkspaceGitDiffResult gitDiff(
            WorkspaceToolScope scope,
            ToolExecutionObserver observer,
            BooleanSupplier cancelled,
            List<ToolExecutionEvidence> evidence,
            AtomicInteger calls,
            Set<String> seenCalls,
            WorkspaceGitDiffInput input) {
        Call call = begin(scope, observer, GIT_DIFF, input, cancelled, calls, seenCalls);
        if (!call.allowed() || input == null || input.path() == null || input.path().isBlank()) {
            finish(scope, observer, evidence, call, ToolExecutionStatus.DENIED);
            return new WorkspaceGitDiffResult(
                    ToolExecutionStatus.DENIED, null, "", false,
                    "Git diff requires one authorized workspace-relative path.");
        }
        try {
            if (scope.localDevice()) {
                WorkspaceGitDiffResult result = localGateway.gitDiff(scope, input);
                finish(scope, observer, evidence, call, result.status());
                return result;
            }
            String relativePath = input.path().trim();
            String diff = git.diff(workspace(scope), relativePath);
            ToolExecutionStatus status = diff.isBlank()
                    ? ToolExecutionStatus.NO_RESULTS
                    : ToolExecutionStatus.FOUND;
            finish(scope, observer, evidence, call, status);
            return new WorkspaceGitDiffResult(
                    status, relativePath, diff, false,
                    diff.isBlank() ? "No unstaged diff was found for this file." : "Git diff found.");
        } catch (RuntimeException exception) {
            ToolExecutionStatus status = finishException(scope, observer, evidence, call, exception);
            return new WorkspaceGitDiffResult(
                    status, null, "", false,
                    status == ToolExecutionStatus.DENIED
                            ? "Git diff was denied by the content policy."
                            : "Git diff is unavailable for this path.");
        }
    }

    private WorkspaceInspectProjectResult inspectProject(
            WorkspaceToolScope scope,
            ToolExecutionObserver observer,
            BooleanSupplier cancelled,
            List<ToolExecutionEvidence> evidence,
            AtomicInteger calls,
            Set<String> seenCalls,
            WorkspaceProjectQueryInput input) {
        Call call = begin(scope, observer, INSPECT_PROJECT, input, cancelled, calls, seenCalls);
        if (!call.allowed()) {
            finish(scope, observer, evidence, call, ToolExecutionStatus.DENIED);
            return new WorkspaceInspectProjectResult(
                    ToolExecutionStatus.DENIED, scope.workspaceName(), List.of(), null,
                    "Project inspection was denied by the request policy.");
        }
        try {
            if (scope.localDevice()) {
                WorkspaceInspectProjectResult result = localGateway.inspectProject(scope, input);
                finish(scope, observer, evidence, call, result.status());
                return result;
            }
            Workspace workspace = workspace(scope);
            var root = pathResolver.workspaceRoot(workspace);
            WorkspaceInspectProjectResult result = new WorkspaceInspectProjectResult(
                    ToolExecutionStatus.COMPLETED,
                    scope.workspaceName(),
                    inspection.detectStack(root),
                    inspection.gitSummary(root).orElse(null),
                    "Project inspection completed.");
            finish(scope, observer, evidence, call, ToolExecutionStatus.COMPLETED);
            return result;
        } catch (RuntimeException exception) {
            ToolExecutionStatus status = finishException(scope, observer, evidence, call, exception);
            return new WorkspaceInspectProjectResult(
                    status, scope.workspaceName(), List.of(), null,
                    "Project inspection is unavailable.");
        }
    }

    private Call begin(
            WorkspaceToolScope scope,
            ToolExecutionObserver observer,
            String toolName,
            Object input,
            BooleanSupplier cancelled,
            AtomicInteger calls,
            Set<String> seenCalls) {
        String argumentsDigest = digest(input == null ? "null" : input.toString());
        Call call = new Call(UUID.randomUUID(), toolName, clock.instant(),
                scope.available()
                        && !cancelled.getAsBoolean()
                        && calls.incrementAndGet() <= MAX_CALLS
                        && seenCalls.add(toolName + ":" + argumentsDigest));
        observer.onStarted(new ToolExecutionStarted(
                call.executionId(), toolName, argumentsDigest, call.startedAt()));
        audit(scope, AuditAction.TOOL_CALL_STARTED, AuditOutcome.SUCCESS, toolName);
        return call;
    }

    private Workspace workspace(WorkspaceToolScope scope) {
        Workspace workspace = access.accessibleWorkspace(scope.userId(), scope.workspaceId());
        if (!workspace.isBound() || access.lightStatus(workspace) != WorkspaceStatus.AVAILABLE) {
            throw new WorkspaceUnavailableException();
        }
        return workspace;
    }

    private Workspace writableWorkspace(WorkspaceToolScope scope) {
        Workspace workspace = workspace(scope);
        if (!scope.writeAuthorized() || !workspace.getAccessMode().allowsWrite()) {
            throw new WorkspaceAccessDeniedException();
        }
        return workspace;
    }

    private WorkspaceWriteFileResult failedWrite(ToolExecutionStatus status, String message) {
        return new WorkspaceWriteFileResult(status, null, false, 0L, null, message);
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String digestBytes(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private WorkspaceListFilesResult failedList(
            WorkspaceToolScope scope,
            ToolExecutionObserver observer,
            List<ToolExecutionEvidence> evidence,
            Call call,
            RuntimeException exception) {
        ToolExecutionStatus status = finishException(scope, observer, evidence, call, exception);
        return new WorkspaceListFilesResult(
                status, "", List.of(), List.of(), false, null,
                status == ToolExecutionStatus.DENIED
                        ? "Workspace listing was denied by the content policy."
                        : "Workspace listing is unavailable.");
    }

    private WorkspaceReadFileResult failedRead(ToolExecutionStatus status, String message) {
        return new WorkspaceReadFileResult(status, null, "", 0, 0, 0, null, false, message);
    }

    private ToolExecutionStatus finishException(
            WorkspaceToolScope scope,
            ToolExecutionObserver observer,
            List<ToolExecutionEvidence> evidence,
            Call call,
            RuntimeException exception) {
        ToolExecutionStatus status = exception instanceof WorkspaceAccessDeniedException
                        || exception instanceof WorkspaceInvalidPathException
                        || exception instanceof WorkspaceFileNotTextException
                        || exception instanceof WorkspaceFileTooLargeException
                ? ToolExecutionStatus.DENIED
                : ToolExecutionStatus.UNAVAILABLE;
        if (status == ToolExecutionStatus.UNAVAILABLE) {
            log.warn("[NEXO-BACK][WORKSPACE] Read tool failed workspaceId={} tool={} reason={}",
                    scope.workspaceId(), call.toolName(), exception.getClass().getSimpleName());
        }
        finish(scope, observer, evidence, call, status);
        return status;
    }

    private void finish(
            WorkspaceToolScope scope,
            ToolExecutionObserver observer,
            List<ToolExecutionEvidence> evidence,
            Call call,
            ToolExecutionStatus status) {
        Instant completedAt = clock.instant();
        ToolExecutionEvidence completed = new ToolExecutionEvidence(
                call.executionId(),
                call.toolName(),
                status,
                Math.max(0L, completedAt.toEpochMilli() - call.startedAt().toEpochMilli()),
                List.of(),
                completedAt);
        evidence.add(completed);
        observer.onCompleted(completed);
        AuditAction action = switch (status) {
            case DENIED -> AuditAction.TOOL_CALL_DENIED;
            case FAILED, UNAVAILABLE -> AuditAction.TOOL_CALL_FAILED;
            default -> AuditAction.TOOL_CALL_COMPLETED;
        };
        AuditOutcome outcome = status == ToolExecutionStatus.FAILED || status == ToolExecutionStatus.UNAVAILABLE
                ? AuditOutcome.FAILURE
                : AuditOutcome.SUCCESS;
        audit(scope, action, outcome, call.toolName() + ":" + status.name());
    }

    private void audit(
            WorkspaceToolScope scope,
            AuditAction action,
            AuditOutcome outcome,
            String detail) {
        audit.record(new RecordAuditCommand(
                action, outcome, scope.userId(), null, AuditTargetType.WORKSPACE,
                scope.workspaceId(), scope.correlationId(), detail));
    }

    private List<String> safeChangedPaths(String rawStatus) {
        return rawStatus.lines()
                .filter(line -> !line.startsWith("##"))
                .filter(line -> line.length() > 3)
                .map(line -> line.substring(3).trim())
                .map(path -> path.contains(" -> ") ? path.substring(path.indexOf(" -> ") + 4) : path)
                .filter(path -> {
                    try {
                        contentPolicy.requireReadable(path);
                        return true;
                    } catch (RuntimeException exception) {
                        return false;
                    }
                })
                .limit(500)
                .toList();
    }

    private String numbered(String content, int startLine) {
        if (content.isEmpty()) {
            return "";
        }
        String[] lines = content.split("\n", -1);
        StringBuilder numbered = new StringBuilder();
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) {
                numbered.append('\n');
            }
            numbered.append(Math.max(1, startLine) + index).append(" | ").append(lines[index]);
        }
        return numbered.toString();
    }

    private String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record Call(
            UUID executionId,
            String toolName,
            Instant startedAt,
            boolean allowed) {}
}
