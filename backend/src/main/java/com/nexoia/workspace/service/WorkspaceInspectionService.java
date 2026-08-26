package com.nexoia.workspace.service;

import com.nexoia.workspace.config.WorkspaceProperties;
import com.nexoia.workspace.dto.WorkspaceFileResponse;
import com.nexoia.workspace.dto.WorkspaceGitSummary;
import com.nexoia.workspace.dto.WorkspaceOmissionResponse;
import com.nexoia.workspace.dto.WorkspaceStatusResponse;
import com.nexoia.workspace.dto.WorkspaceTreeEntryResponse;
import com.nexoia.workspace.dto.WorkspaceTreeResponse;
import com.nexoia.workspace.exception.WorkspaceAccessDeniedException;
import com.nexoia.workspace.exception.WorkspaceFileNotTextException;
import com.nexoia.workspace.exception.WorkspaceFileTooLargeException;
import com.nexoia.workspace.exception.WorkspaceInvalidPathException;
import com.nexoia.workspace.exception.WorkspaceUnavailableException;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.model.WorkspaceEntryType;
import com.nexoia.workspace.model.WorkspaceStatus;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Reads a bound Workspace's structure without ever exposing an absolute path. It lists directories
 * lazily and bounded, derives a deterministic structure fingerprint used to detect external changes,
 * reads safe Git metadata from the {@code .git} directory, and detects the project stack from its
 * manifests. It never follows a symlink out of the Workspace and never returns binary content.
 */
@Service
@RequiredArgsConstructor
public class WorkspaceInspectionService {

    /** Manifest file name -> stack label, for lightweight project detection. */
    private static final Map<String, String> STACK_MANIFESTS = Map.ofEntries(
            Map.entry("pom.xml", "maven"),
            Map.entry("build.gradle", "gradle"),
            Map.entry("build.gradle.kts", "gradle"),
            Map.entry("settings.gradle", "gradle"),
            Map.entry("package.json", "node"),
            Map.entry("pnpm-lock.yaml", "node"),
            Map.entry("requirements.txt", "python"),
            Map.entry("pyproject.toml", "python"),
            Map.entry("go.mod", "go"),
            Map.entry("Cargo.toml", "rust"),
            Map.entry("compose.yaml", "docker-compose"),
            Map.entry("docker-compose.yml", "docker-compose"),
            Map.entry("Dockerfile", "docker"));

    private static final int FINGERPRINT_ENTRY_CAP = 20_000;
    private static final int BINARY_SNIFF_BYTES = 8_192;

    private final WorkspaceProperties properties;
    private final WorkspacePathResolver pathResolver;
    private final WorkspaceContentPolicy contentPolicy;

    /** Lists one Workspace-relative directory, paginated by an opaque cursor and bounded server-side. */
    public WorkspaceTreeResponse tree(Workspace workspace, String relativePath, Integer limit, String cursor) {
        Path root = pathResolver.workspaceRoot(workspace);
        String normalizedRelative = relativePath == null || relativePath.isBlank() ? "" : relativePath;
        Path directory = normalizedRelative.isEmpty()
                ? requireDirectory(root)
                : requireDirectory(pathResolver.resolveExisting(workspace, normalizedRelative));
        if (!normalizedRelative.isEmpty()
                && (contentPolicy.isIgnored(root, directory)
                        || contentPolicy.isSensitive(normalizedRelative))) {
            throw new WorkspaceAccessDeniedException();
        }

        int cappedLimit = boundedLimit(limit);
        List<WorkspaceTreeEntryResponse> entries = new ArrayList<>();
        List<WorkspaceOmissionResponse> omissions = new ArrayList<>();
        String nextCursor = null;
        boolean truncated = false;

        // The cursor is an index into the deterministic sorted listing, so pagination is independent of
        // the directories-first ordering that a name comparison would break.
        List<Path> children = sortedChildren(directory);
        int index = cursorIndex(cursor);
        while (index < children.size()) {
            Path child = children.get(index);
            String name = child.getFileName().toString();
            if (Files.isSymbolicLink(child)) {
                omissions.add(new WorkspaceOmissionResponse(name, "symlink"));
                index++;
                continue;
            }
            if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)
                    && contentPolicy.isIgnoredDirectoryName(name)) {
                omissions.add(new WorkspaceOmissionResponse(name, "ignored"));
                index++;
                continue;
            }
            if (contentPolicy.isSensitive(relative(root, child))) {
                omissions.add(new WorkspaceOmissionResponse(name, "sensitive"));
                index++;
                continue;
            }
            if (entries.size() == cappedLimit) {
                truncated = true;
                nextCursor = encodeCursor(index);
                break;
            }
            entries.add(entry(root, child));
            index++;
        }
        return new WorkspaceTreeResponse(normalizedRelative, entries, omissions, truncated, nextCursor);
    }

    /** Full live status: resolves the root, compares the current fingerprint/HEAD against the stored scan. */
    public WorkspaceStatusResponse status(Workspace workspace) {
        if (!workspace.isBound()) {
            return unboundStatus(workspace);
        }
        Path root;
        try {
            root = pathResolver.workspaceRoot(workspace);
        } catch (RuntimeException exception) {
            return errorStatus(workspace, WorkspaceStatus.ERROR, "Workspace storage could not be resolved");
        }
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(root)) {
            return errorStatus(workspace, WorkspaceStatus.MISSING, "Workspace path is missing or unreadable");
        }
        String fingerprint = fingerprint(root);
        Optional<WorkspaceGitSummary> git = gitSummary(root);
        String head = git.map(WorkspaceGitSummary::head).orElse(null);
        WorkspaceStatus status = detectChange(workspace, fingerprint, head)
                ? WorkspaceStatus.CHANGED
                : WorkspaceStatus.AVAILABLE;
        return new WorkspaceStatusResponse(
                status, workspace.getStorageType(), workspace.getAccessMode(), workspace.getRelativePath(),
                fingerprint, workspace.getLastScannedAt(), git.orElse(null), detectStack(root), null);
    }

    /** Recomputes and records the structure fingerprint and Git HEAD, returning the fresh status. */
    public WorkspaceStatusResponse refresh(Workspace workspace) {
        if (!workspace.isBound()) {
            return unboundStatus(workspace);
        }
        Path root = pathResolver.workspaceRoot(workspace);
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(root)) {
            return errorStatus(workspace, WorkspaceStatus.MISSING, "Workspace path is missing or unreadable");
        }
        String fingerprint = fingerprint(root);
        Optional<WorkspaceGitSummary> git = gitSummary(root);
        workspace.recordScan(fingerprint, git.map(WorkspaceGitSummary::head).orElse(null), Instant.now());
        return new WorkspaceStatusResponse(
                WorkspaceStatus.AVAILABLE, workspace.getStorageType(), workspace.getAccessMode(),
                workspace.getRelativePath(), fingerprint, workspace.getLastScannedAt(),
                git.orElse(null), detectStack(root), null);
    }

    /** Bounded, text-only file preview with an optional 1-based line range. Binary files are refused. */
    public WorkspaceFileResponse file(Workspace workspace, String relativePath, Integer startLine, Integer endLine) {
        contentPolicy.requireReadable(relativePath);
        Path root = pathResolver.workspaceRoot(workspace);
        Path file = pathResolver.resolveExisting(workspace, relativePath);
        if (contentPolicy.isIgnored(root, file)
                || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new WorkspaceAccessDeniedException();
        }
        try {
            long size = Files.size(file);
            if (size > properties.maxFileBytes()) {
                throw new WorkspaceFileTooLargeException();
            }
            int readLimit = Math.toIntExact(Math.min(properties.maxFileBytes(), Integer.MAX_VALUE - 1L));
            byte[] bytes;
            try (var input = Files.newInputStream(file)) {
                bytes = input.readNBytes(readLimit + 1);
            }
            if (bytes.length > readLimit) {
                throw new WorkspaceFileTooLargeException();
            }
            if (looksBinary(bytes)) {
                throw new WorkspaceFileNotTextException();
            }
            String content = decodeUtf8(bytes);
            String[] lines = content.isEmpty() ? new String[0] : content.split("\n", -1);
            int totalLines = lines.length;
            int from = startLine == null ? 1 : Math.max(1, startLine);
            int to = endLine == null ? totalLines : Math.min(totalLines, endLine);
            if (from > totalLines) {
                from = totalLines;
            }
            if (to < from) {
                to = from;
            }
            String slice = totalLines == 0 ? "" : String.join("\n", List.of(lines).subList(from - 1, to));
            boolean truncated = from > 1 || to < totalLines;
            return new WorkspaceFileResponse(
                    relativePath, slice, from, to, totalLines, size, sha256(bytes), truncated);
        } catch (IOException exception) {
            throw new WorkspaceUnavailableException();
        }
    }

    /** Deterministic hash of the relevant structure, ignoring heavy/internal directories. */
    public String fingerprint(Path root) {
        TreeMap<String, String> ordered = new TreeMap<>();
        collectFingerprintEntries(root, root, ordered);
        MessageDigest digest = sha256Digest();
        ordered.forEach((relative, descriptor) -> {
            digest.update(relative.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(descriptor.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
        });
        return HexFormat.of().formatHex(digest.digest());
    }

    private void collectFingerprintEntries(
            Path root,
            Path directory,
            TreeMap<String, String> ordered) {
        if (ordered.size() >= FINGERPRINT_ENTRY_CAP) {
            return;
        }
        for (Path child : fingerprintChildren(directory)) {
            if (ordered.size() >= FINGERPRINT_ENTRY_CAP) {
                return;
            }
            String relative = relative(root, child);
            if (Files.isSymbolicLink(child)
                    || contentPolicy.isIgnored(root, child)
                    || contentPolicy.isSensitive(relative)) {
                continue;
            }
            ordered.put(relative, descriptor(child));
            if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                collectFingerprintEntries(root, child, ordered);
            }
        }
    }

    private List<Path> fingerprintChildren(Path directory) {
        try (Stream<Path> children = Files.list(directory)) {
            return children.sorted(Comparator.comparing(
                            path -> path.getFileName().toString()))
                    .toList();
        } catch (IOException exception) {
            throw new WorkspaceUnavailableException();
        }
    }

    /** Reads branch and HEAD from {@code .git} using only file reads — no process is spawned. */
    public Optional<WorkspaceGitSummary> gitSummary(Path root) {
        Path gitDir = root.resolve(".git");
        if (!Files.isDirectory(gitDir, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        Path headFile = gitDir.resolve("HEAD");
        if (!Files.isRegularFile(headFile, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        try {
            String head = Files.readString(headFile, StandardCharsets.UTF_8).trim();
            if (head.startsWith("ref:")) {
                String ref = head.substring(4).trim();
                String branch = ref.startsWith("refs/heads/") ? ref.substring("refs/heads/".length()) : ref;
                return Optional.of(new WorkspaceGitSummary(branch, resolveRef(gitDir, ref), false));
            }
            return Optional.of(new WorkspaceGitSummary(null, head.isBlank() ? null : head, true));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    /** Stack labels detected from manifest files present at the Workspace root. */
    public List<String> detectStack(Path root) {
        return STACK_MANIFESTS.entrySet().stream()
                .filter(entry -> Files.isRegularFile(root.resolve(entry.getKey()), LinkOption.NOFOLLOW_LINKS))
                .map(Map.Entry::getValue)
                .distinct()
                .sorted()
                .toList();
    }

    private String resolveRef(Path gitDir, String ref) {
        Path refFile = gitDir.resolve(ref);
        if (Files.isRegularFile(refFile, LinkOption.NOFOLLOW_LINKS)) {
            try {
                return Files.readString(refFile, StandardCharsets.UTF_8).trim();
            } catch (IOException ignored) {
                return null;
            }
        }
        Path packed = gitDir.resolve("packed-refs");
        if (Files.isRegularFile(packed, LinkOption.NOFOLLOW_LINKS)) {
            try (Stream<String> lines = Files.lines(packed, StandardCharsets.UTF_8)) {
                return lines.filter(line -> line.endsWith(" " + ref))
                        .map(line -> line.substring(0, line.indexOf(' ')))
                        .findFirst()
                        .orElse(null);
            } catch (IOException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean detectChange(Workspace workspace, String fingerprint, String head) {
        boolean structureMoved = workspace.getStructureFingerprint() != null
                && !workspace.getStructureFingerprint().equals(fingerprint);
        boolean headMoved = workspace.getGitHead() != null && !workspace.getGitHead().equals(head);
        return structureMoved || headMoved;
    }

    private List<Path> sortedChildren(Path directory) {
        try (Stream<Path> children = Files.list(directory)) {
            return children.sorted(Comparator
                            .comparing((Path path) -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) ? 0 : 1)
                            .thenComparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (IOException exception) {
            throw new WorkspaceUnavailableException();
        }
    }

    private WorkspaceTreeEntryResponse entry(Path root, Path child) {
        boolean directory = Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS);
        Long size = null;
        Instant modifiedAt = null;
        try {
            if (!directory) {
                size = Files.size(child);
            }
            modifiedAt = Files.getLastModifiedTime(child, LinkOption.NOFOLLOW_LINKS).toInstant();
        } catch (IOException ignored) {
            // A transient stat failure leaves size/modifiedAt null rather than failing the whole listing.
        }
        return new WorkspaceTreeEntryResponse(
                relative(root, child),
                child.getFileName().toString(),
                directory ? WorkspaceEntryType.DIRECTORY : WorkspaceEntryType.FILE,
                size,
                modifiedAt);
    }

    private Path requireDirectory(Path path) {
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new WorkspaceAccessDeniedException();
        }
        return path;
    }

    private String descriptor(Path path) {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            return "d";
        }
        try {
            return "f:" + Files.size(path) + ":"
                    + Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toMillis();
        } catch (IOException exception) {
            return "f:?";
        }
    }

    private String relative(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    private int boundedLimit(Integer limit) {
        int requested = limit == null ? properties.maxTreeEntries() : limit;
        return Math.max(1, Math.min(requested, properties.maxTreeEntries()));
    }

    private int cursorIndex(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            int index = Integer.parseInt(new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8));
            return Math.max(0, index);
        } catch (IllegalArgumentException exception) {
            throw new WorkspaceInvalidPathException();
        }
    }

    private String encodeCursor(int index) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Integer.toString(index).getBytes(StandardCharsets.UTF_8));
    }

    private boolean looksBinary(byte[] bytes) {
        int limit = Math.min(bytes.length, BINARY_SNIFF_BYTES);
        for (int index = 0; index < limit; index++) {
            if (bytes[index] == 0) {
                return true;
            }
        }
        return false;
    }

    private String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            throw new WorkspaceFileNotTextException();
        }
    }

    private String sha256(byte[] bytes) {
        return HexFormat.of().formatHex(sha256Digest().digest(bytes));
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new UncheckedIOException(new IOException("SHA-256 unavailable", exception));
        }
    }

    private WorkspaceStatusResponse unboundStatus(Workspace workspace) {
        return new WorkspaceStatusResponse(
                WorkspaceStatus.UNBOUND, workspace.getStorageType(), workspace.getAccessMode(),
                workspace.getRelativePath(), null, workspace.getLastScannedAt(), null, List.of(), null);
    }

    private WorkspaceStatusResponse errorStatus(Workspace workspace, WorkspaceStatus status, String reason) {
        return new WorkspaceStatusResponse(
                status, workspace.getStorageType(), workspace.getAccessMode(), workspace.getRelativePath(),
                workspace.getStructureFingerprint(), workspace.getLastScannedAt(), null, List.of(), reason);
    }
}
