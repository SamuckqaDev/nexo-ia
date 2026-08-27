package com.nexoia.workspace.change.service;

import com.nexoia.conversation.chat.exception.ConversationNotFoundException;
import com.nexoia.conversation.chat.repository.ConversationRepository;
import com.nexoia.provider.dto.ToolExecutionStatus;
import com.nexoia.provider.dto.WorkspaceToolScope;
import com.nexoia.workspace.change.dto.WorkspaceChangeResponse;
import com.nexoia.workspace.change.exception.WorkspaceChangeNotFoundException;
import com.nexoia.workspace.change.exception.WorkspaceChangeStateException;
import com.nexoia.workspace.change.model.WorkspaceChangeArtifact;
import com.nexoia.workspace.change.model.WorkspaceChangeOperation;
import com.nexoia.workspace.change.model.WorkspaceChangeStatus;
import com.nexoia.workspace.change.repository.WorkspaceChangeArtifactRepository;
import com.nexoia.workspace.change.service.WorkspaceChangeArtifactStore.StoredProposal;
import com.nexoia.workspace.config.WorkspaceProperties;
import com.nexoia.workspace.exception.WorkspaceAccessDeniedException;
import com.nexoia.workspace.exception.WorkspaceChangedException;
import com.nexoia.workspace.exception.WorkspaceFileNotTextException;
import com.nexoia.workspace.exception.WorkspaceFileTooLargeException;
import com.nexoia.workspace.exception.WorkspaceInvalidPathException;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.model.WorkspaceBinding;
import com.nexoia.workspace.service.WorkspaceAccessService;
import com.nexoia.workspace.service.WorkspaceBindingService;
import com.nexoia.workspace.service.WorkspaceContentPolicy;
import com.nexoia.workspace.service.WorkspacePathResolver;
import com.nexoia.workspace.tool.LocalWorkspaceToolGateway;
import com.nexoia.workspace.tool.WorkspaceApplyPatchInput;
import com.nexoia.workspace.tool.WorkspaceChangeProposalResult;
import com.nexoia.workspace.tool.WorkspaceCreateFileInput;
import com.nexoia.workspace.tool.WorkspaceDeleteFileInput;
import com.nexoia.workspace.tool.WorkspaceDeleteFileResult;
import com.nexoia.workspace.tool.WorkspaceDeleteFileRuntimeInput;
import com.nexoia.workspace.tool.WorkspaceRawFileResult;
import com.nexoia.workspace.tool.WorkspaceReadFileInput;
import com.nexoia.workspace.tool.WorkspaceWriteFileInput;
import com.nexoia.workspace.tool.WorkspaceWriteFileResult;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
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
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates server-side previews and applies only the exact preview explicitly approved by the user. */
@Service
public class WorkspaceChangeService {

    private static final int PREVIEW_LIMIT = 120_000;

    private final WorkspaceChangeArtifactRepository changes;
    private final ConversationRepository conversations;
    private final WorkspaceAccessService access;
    private final WorkspaceBindingService bindings;
    private final WorkspacePathResolver pathResolver;
    private final WorkspaceContentPolicy contentPolicy;
    private final WorkspaceProperties properties;
    private final WorkspaceChangeArtifactStore artifactStore;
    private final LocalWorkspaceToolGateway localGateway;
    private final Clock clock;

    public WorkspaceChangeService(
            WorkspaceChangeArtifactRepository changes,
            ConversationRepository conversations,
            WorkspaceAccessService access,
            WorkspaceBindingService bindings,
            WorkspacePathResolver pathResolver,
            WorkspaceContentPolicy contentPolicy,
            WorkspaceProperties properties,
            WorkspaceChangeArtifactStore artifactStore,
            LocalWorkspaceToolGateway localGateway,
            Clock clock) {
        this.changes = changes;
        this.conversations = conversations;
        this.access = access;
        this.bindings = bindings;
        this.pathResolver = pathResolver;
        this.contentPolicy = contentPolicy;
        this.properties = properties;
        this.artifactStore = artifactStore;
        this.localGateway = localGateway;
        this.clock = clock;
    }

    @Transactional
    public WorkspaceChangeProposalResult proposePatch(WorkspaceToolScope scope, WorkspaceApplyPatchInput input) {
        requireWritableScope(scope);
        if (input == null || blank(input.path()) || blank(input.oldString()) || input.newString() == null) {
            throw new WorkspaceInvalidPathException();
        }
        CurrentFile current = readCurrent(scope, input.path());
        if (input.oldString().equals(input.newString())) {
            throw new WorkspaceChangeStateException("The requested edit does not change any bytes");
        }
        int occurrences = countOccurrences(current.content(), input.oldString());
        if (occurrences == 0) {
            throw new WorkspaceChangeStateException("The exact oldString was not found in the current file");
        }
        boolean replaceAll = Boolean.TRUE.equals(input.replaceAll());
        if (!replaceAll && occurrences > 1) {
            throw new WorkspaceChangeStateException(
                    "oldString matches multiple locations; include more surrounding context");
        }
        String after = replaceAll
                ? current.content().replace(input.oldString(), input.newString())
                : replaceFirst(current.content(), input.oldString(), input.newString());
        requireBoundedText(after);
        return persistProposal(
                scope,
                WorkspaceChangeOperation.EDIT,
                input.path().trim(),
                current,
                after,
                replaceAll ? occurrences : 1);
    }

    @Transactional
    public WorkspaceChangeProposalResult proposeCreate(WorkspaceToolScope scope, WorkspaceCreateFileInput input) {
        requireWritableScope(scope);
        if (input == null || blank(input.path()) || input.content() == null) {
            throw new WorkspaceInvalidPathException();
        }
        requireBoundedText(input.content());
        String path = input.path().trim();
        contentPolicy.requireReadable(path);
        if (!scope.localDevice()) {
            Workspace workspace = writableWorkspace(scope);
            Path candidate = pathResolver.resolveForCreate(workspace, path);
            if (Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
                throw new WorkspaceChangeStateException("The target file already exists; use workspace_apply_patch");
            }
        }
        return persistProposal(scope, WorkspaceChangeOperation.CREATE, path, null, input.content(), null);
    }

    @Transactional
    public WorkspaceChangeProposalResult proposeDelete(WorkspaceToolScope scope, WorkspaceDeleteFileInput input) {
        requireWritableScope(scope);
        if (input == null || blank(input.path())) {
            throw new WorkspaceInvalidPathException();
        }
        CurrentFile current = readCurrent(scope, input.path());
        return persistProposal(
                scope, WorkspaceChangeOperation.DELETE, input.path().trim(), current, null, null);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceChangeResponse> list(UUID userId, UUID conversationId) {
        conversations.findByIdAndUserIdAndArchivedFalse(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);
        return changes.findAllByUserIdAndConversationIdOrderByCreatedAtDesc(userId, conversationId).stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public WorkspaceChangeResponse approve(UUID userId, UUID changeId) {
        WorkspaceChangeArtifact change = ownedChange(userId, changeId);
        if (change.getStatus() != WorkspaceChangeStatus.PENDING_APPROVAL) {
            throw new WorkspaceChangeStateException("Only a pending workspace change can be approved");
        }
        WorkspaceToolScope scope = scope(change);
        try {
            apply(scope, change);
            change.apply(clock.instant());
        } catch (WorkspaceChangedException exception) {
            change.invalidate("WORKSPACE_CHANGED", clock.instant());
        } catch (RuntimeException exception) {
            change.fail("WORKSPACE_CHANGE_FAILED", clock.instant());
        }
        return response(change);
    }

    @Transactional
    public WorkspaceChangeResponse deny(UUID userId, UUID changeId) {
        WorkspaceChangeArtifact change = ownedChange(userId, changeId);
        if (change.getStatus() != WorkspaceChangeStatus.PENDING_APPROVAL) {
            throw new WorkspaceChangeStateException("Only a pending workspace change can be denied");
        }
        change.deny(clock.instant());
        return response(change);
    }

    @Transactional
    public WorkspaceChangeResponse revert(UUID userId, UUID changeId) {
        WorkspaceChangeArtifact change = ownedChange(userId, changeId);
        if (change.getStatus() != WorkspaceChangeStatus.APPLIED) {
            throw new WorkspaceChangeStateException("Only an applied workspace change can be reverted");
        }
        WorkspaceToolScope scope = scope(change);
        try {
            rollback(scope, change);
            change.revert(clock.instant());
        } catch (WorkspaceChangedException exception) {
            change.invalidate("ROLLBACK_CONFLICT", clock.instant());
        } catch (RuntimeException exception) {
            change.fail("WORKSPACE_ROLLBACK_FAILED", clock.instant());
        }
        return response(change);
    }

    private WorkspaceChangeProposalResult persistProposal(
            WorkspaceToolScope scope,
            WorkspaceChangeOperation operation,
            String path,
            CurrentFile before,
            String afterContent,
            Integer replacementCount) {
        UUID id = UUID.randomUUID();
        String beforeContent = before == null ? null : before.content();
        StoredProposal stored = artifactStore.write(
                scope.userId(), scope.conversationId(), id, beforeContent, afterContent);
        String beforeSha = before == null ? null : before.sha256();
        String afterSha = afterContent == null ? null : digest(afterContent);
        WorkspaceChangeArtifact change = changes.saveAndFlush(WorkspaceChangeArtifact.builder()
                .id(id)
                .userId(scope.userId())
                .conversationId(scope.conversationId())
                .assistantMessageId(scope.assistantMessageId())
                .correlationId(scope.correlationId())
                .workspaceId(scope.workspaceId())
                .workspaceBindingId(scope.workspaceBindingId())
                .operation(operation)
                .status(WorkspaceChangeStatus.PENDING_APPROVAL)
                .relativePath(path)
                .beforeSha256(beforeSha)
                .afterSha256(afterSha)
                .beforeArtifactKey(stored.beforeKey())
                .afterArtifactKey(stored.afterKey())
                .replacementCount(replacementCount)
                .build());
        return new WorkspaceChangeProposalResult(
                ToolExecutionStatus.COMPLETED,
                change.getId(),
                operation,
                path,
                beforeSha,
                afterSha,
                replacementCount,
                true,
                "Server-generated change preview created. Approval is required in Artifacts before any file changes.");
    }

    private void apply(WorkspaceToolScope scope, WorkspaceChangeArtifact change) {
        String after = artifactStore.read(change.getAfterArtifactKey());
        if (change.getOperation() == WorkspaceChangeOperation.CREATE) {
            write(scope, change.getRelativePath(), after, null);
            return;
        }
        CurrentFile current = readCurrent(scope, change.getRelativePath());
        requireHash(current.sha256(), change.getBeforeSha256());
        if (change.getOperation() == WorkspaceChangeOperation.EDIT) {
            write(scope, change.getRelativePath(), after, current.sha256());
        } else {
            delete(scope, change.getRelativePath(), current.sha256());
        }
    }

    private void rollback(WorkspaceToolScope scope, WorkspaceChangeArtifact change) {
        if (change.getOperation() == WorkspaceChangeOperation.CREATE) {
            CurrentFile current = readCurrent(scope, change.getRelativePath());
            requireHash(current.sha256(), change.getAfterSha256());
            delete(scope, change.getRelativePath(), current.sha256());
            return;
        }
        String before = artifactStore.read(change.getBeforeArtifactKey());
        if (change.getOperation() == WorkspaceChangeOperation.EDIT) {
            CurrentFile current = readCurrent(scope, change.getRelativePath());
            requireHash(current.sha256(), change.getAfterSha256());
            write(scope, change.getRelativePath(), before, current.sha256());
            return;
        }
        write(scope, change.getRelativePath(), before, null);
    }

    private CurrentFile readCurrent(WorkspaceToolScope scope, String rawPath) {
        String path = rawPath.trim();
        contentPolicy.requireReadable(path);
        if (scope.localDevice()) {
            WorkspaceRawFileResult result = localGateway.readRawFile(
                    scope, new WorkspaceReadFileInput(path, null, null));
            if (result.status() != ToolExecutionStatus.COMPLETED) {
                throw new WorkspaceChangedException();
            }
            requireBoundedText(result.content());
            return new CurrentFile(result.content(), result.sha256());
        }
        Workspace workspace = writableWorkspace(scope);
        Path target = pathResolver.resolveExisting(workspace, path);
        if (Files.isSymbolicLink(target) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new WorkspaceAccessDeniedException();
        }
        try {
            if (Files.size(target) > properties.maxFileBytes()) {
                throw new WorkspaceFileTooLargeException();
            }
            byte[] bytes = Files.readAllBytes(target);
            if (containsNul(bytes)) {
                throw new WorkspaceFileNotTextException();
            }
            String content = decodeUtf8(bytes);
            return new CurrentFile(content, digest(bytes));
        } catch (IOException exception) {
            throw new WorkspaceChangedException();
        }
    }

    private void write(WorkspaceToolScope scope, String path, String content, String expectedSha) {
        if (scope.localDevice()) {
            WorkspaceWriteFileResult result = localGateway.writeFile(
                    scope, new WorkspaceWriteFileInput(path, content, expectedSha));
            if (result.status() != ToolExecutionStatus.COMPLETED) {
                throw new WorkspaceChangedException();
            }
            return;
        }
        Workspace workspace = writableWorkspace(scope);
        Path candidate = pathResolver.resolveForCreate(workspace, path);
        boolean exists = Files.exists(candidate, LinkOption.NOFOLLOW_LINKS);
        Path target = exists ? pathResolver.resolveExisting(workspace, path) : candidate;
        if (exists) {
            try {
                requireHash(digest(Files.readAllBytes(target)), expectedSha);
            } catch (IOException exception) {
                throw new WorkspaceChangedException();
            }
        } else if (expectedSha != null) {
            throw new WorkspaceChangedException();
        }
        Path parent = target.getParent();
        if (parent == null || !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
            throw new WorkspaceInvalidPathException();
        }
        Path temporary = null;
        try {
            temporary = Files.createTempFile(parent, ".nexo-change-", ".tmp");
            Files.writeString(temporary, content, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            moveAtomically(temporary, target);
        } catch (IOException exception) {
            throw new WorkspaceChangedException();
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Best effort cleanup; the target promotion already decided the operation result.
                }
            }
        }
    }

    private void delete(WorkspaceToolScope scope, String path, String expectedSha) {
        if (scope.localDevice()) {
            WorkspaceDeleteFileResult result = localGateway.deleteFile(
                    scope, new WorkspaceDeleteFileRuntimeInput(path, expectedSha));
            if (result.status() != ToolExecutionStatus.COMPLETED) {
                throw new WorkspaceChangedException();
            }
            return;
        }
        Workspace workspace = writableWorkspace(scope);
        Path target = pathResolver.resolveExisting(workspace, path);
        pathResolver.requireDeletableTarget(workspace, target);
        try {
            requireHash(digest(Files.readAllBytes(target)), expectedSha);
            Files.delete(target);
        } catch (IOException exception) {
            throw new WorkspaceChangedException();
        }
    }

    private WorkspaceToolScope scope(WorkspaceChangeArtifact change) {
        Workspace workspace = access.accessibleWorkspace(change.getUserId(), change.getWorkspaceId());
        WorkspaceBinding binding = change.getWorkspaceBindingId() == null
                ? null
                : bindings.ownedBinding(
                        change.getUserId(), change.getWorkspaceId(), change.getWorkspaceBindingId());
        if (binding != null && !bindings.isAvailable(change.getUserId(), binding)) {
            throw new WorkspaceChangedException();
        }
        return new WorkspaceToolScope(
                change.getUserId(),
                change.getConversationId(),
                change.getAssistantMessageId(),
                change.getCorrelationId(),
                workspace.getId(),
                workspace.getName(),
                workspace.getAccessMode(),
                true,
                binding == null ? null : binding.getId(),
                binding == null ? null : binding.getDeviceId(),
                binding == null ? null : binding.getLocalBindingId(),
                true);
    }

    private Workspace writableWorkspace(WorkspaceToolScope scope) {
        Workspace workspace = access.accessibleWorkspace(scope.userId(), scope.workspaceId());
        if (!scope.writeAuthorized() || !workspace.getAccessMode().allowsWrite()) {
            throw new WorkspaceAccessDeniedException();
        }
        return workspace;
    }

    private void requireWritableScope(WorkspaceToolScope scope) {
        if (scope == null || !scope.available() || !scope.writeAuthorized()) {
            throw new WorkspaceAccessDeniedException();
        }
        writableWorkspace(scope);
    }

    private WorkspaceChangeArtifact ownedChange(UUID userId, UUID changeId) {
        return changes.findByIdAndUserId(changeId, userId)
                .orElseThrow(WorkspaceChangeNotFoundException::new);
    }

    private WorkspaceChangeResponse response(WorkspaceChangeArtifact change) {
        String before = artifactStore.read(change.getBeforeArtifactKey());
        String after = artifactStore.read(change.getAfterArtifactKey());
        boolean truncated = length(before) > PREVIEW_LIMIT || length(after) > PREVIEW_LIMIT;
        return new WorkspaceChangeResponse(
                change.getId(),
                change.getWorkspaceId(),
                change.getOperation(),
                change.getStatus(),
                change.getRelativePath(),
                change.getBeforeSha256(),
                change.getAfterSha256(),
                change.getReplacementCount(),
                truncate(before),
                truncate(after),
                truncated,
                change.getFailureCode(),
                change.getCreatedAt(),
                change.getAppliedAt(),
                change.getRevertedAt());
    }

    private void requireBoundedText(String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > properties.maxFileBytes()) {
            throw new WorkspaceFileTooLargeException();
        }
        if (containsNul(bytes)) {
            throw new WorkspaceFileNotTextException();
        }
    }

    private void requireHash(String actual, String expected) {
        if (expected == null || !actual.equalsIgnoreCase(expected)) {
            throw new WorkspaceChangedException();
        }
    }

    private int countOccurrences(String content, String needle) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private String replaceFirst(String content, String oldString, String newString) {
        int index = content.indexOf(oldString);
        return content.substring(0, index) + newString + content.substring(index + oldString.length());
    }

    private String digest(String content) {
        return digest(content.getBytes(StandardCharsets.UTF_8));
    }

    private String digest(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private boolean containsNul(byte[] bytes) {
        for (byte value : bytes) {
            if (value == 0) {
                return true;
            }
        }
        return false;
    }

    private String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new WorkspaceFileNotTextException();
        }
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }

    private String truncate(String value) {
        return value == null || value.length() <= PREVIEW_LIMIT ? value : value.substring(0, PREVIEW_LIMIT);
    }

    private record CurrentFile(String content, String sha256) {}
}
