package com.nexoia.workspace.service;

import com.nexoia.workspace.config.WorkspaceProperties;
import com.nexoia.workspace.exception.WorkspaceAccessDeniedException;
import com.nexoia.workspace.exception.WorkspaceInvalidPathException;
import com.nexoia.workspace.exception.WorkspaceNotBoundException;
import com.nexoia.workspace.exception.WorkspaceUnavailableException;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.model.WorkspaceStorageType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The single, centralized authority that turns a bound Workspace plus an untrusted relative path into
 * a canonical server path guaranteed to stay inside the Workspace root. Controllers, tools and the
 * command runner must all resolve through this component so the containment rules can never drift.
 *
 * <p>No absolute path ever leaves this class toward the model, the API or logs — callers receive a
 * {@link Path} they use server-side only.
 */
@Component
@RequiredArgsConstructor
public class WorkspacePathResolver {

    private static final int MAX_RELATIVE_PATH_LENGTH = 1024;

    private final WorkspaceProperties properties;

    /**
     * Resolves the containing root of a bound Workspace. MANAGED derives from owner and id; MOUNTED
     * derives from the configured import root and the stored relative path. Existence is not required
     * here — the caller decides how a missing root maps to status.
     */
    public Path workspaceRoot(Workspace workspace) {
        if (!workspace.isBound()) {
            throw new WorkspaceNotBoundException();
        }
        if (workspace.getStorageType() == WorkspaceStorageType.MANAGED) {
            return properties.managedRootPath()
                    .resolve(workspace.getOwnerId().toString())
                    .resolve(workspace.getId().toString())
                    .normalize();
        }
        Path importRoot = properties.importRootPath().orElseThrow(WorkspaceUnavailableException::new);
        String relativePath = workspace.getRelativePath();
        if (relativePath == null || relativePath.isBlank()) {
            throw new WorkspaceUnavailableException();
        }
        validateRelativeSegments(relativePath);
        Path root = importRoot.resolve(relativePath).normalize();
        if (!root.startsWith(importRoot)) {
            throw new WorkspaceAccessDeniedException();
        }
        // A MOUNTED root that exists must resolve, through every symlink, back inside the import root.
        if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
            requireInsideRealRoot(importRoot, root);
        }
        return root;
    }

    /** Resolves an existing, readable path inside the Workspace, rejecting any symlink escape. */
    public Path resolveExisting(Workspace workspace, String relativePath) {
        Path root = workspaceRoot(workspace);
        Path candidate = resolveInsideRoot(root, relativePath);
        if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
            throw new WorkspaceAccessDeniedException();
        }
        requireInsideRealRoot(root, candidate);
        return candidate;
    }

    /**
     * Resolves a not-yet-existing target for creation. The nearest existing ancestor is validated so a
     * new file can never be planted outside the root through a symlinked parent directory.
     */
    public Path resolveForCreate(Workspace workspace, String relativePath) {
        Path root = workspaceRoot(workspace);
        Path candidate = resolveInsideRoot(root, relativePath);
        if (candidate.equals(root)) {
            throw new WorkspaceInvalidPathException();
        }
        requireInsideRealRoot(root, nearestExistingAncestor(root, candidate));
        return candidate;
    }

    /**
     * Guards a deletion target: it must be a real path strictly inside the root and never the root,
     * a mount, a home directory or the filesystem root itself.
     */
    public void requireDeletableTarget(Workspace workspace, Path target) {
        Path root = workspaceRoot(workspace);
        Path normalized = target.toAbsolutePath().normalize();
        if (normalized.equals(root) || !normalized.startsWith(root) || normalized.getParent() == null) {
            throw new WorkspaceAccessDeniedException();
        }
        requireInsideRealRoot(root, normalized);
    }

    private Path resolveInsideRoot(Path root, String relativePath) {
        validateRelativeSegments(relativePath);
        Path candidate = root.resolve(relativePath).normalize();
        if (!candidate.startsWith(root)) {
            throw new WorkspaceAccessDeniedException();
        }
        return candidate;
    }

    private void validateRelativeSegments(String relativePath) {
        if (relativePath == null) {
            throw new WorkspaceInvalidPathException();
        }
        if (relativePath.length() > MAX_RELATIVE_PATH_LENGTH || relativePath.indexOf('\u0000') >= 0) {
            throw new WorkspaceInvalidPathException();
        }
        if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
            throw new WorkspaceInvalidPathException();
        }
        // A Windows drive or UNC prefix (e.g. C:\ or \\host) is an absolute reference, never relative.
        if (relativePath.length() >= 2 && relativePath.charAt(1) == ':') {
            throw new WorkspaceInvalidPathException();
        }
        for (String segment : relativePath.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")
                    || segment.equals("~") || segment.indexOf('\\') >= 0) {
                throw new WorkspaceInvalidPathException();
            }
        }
    }

    private Path nearestExistingAncestor(Path root, Path candidate) {
        Path ancestor = candidate.getParent();
        while (ancestor != null && ancestor.startsWith(root)) {
            if (Files.exists(ancestor, LinkOption.NOFOLLOW_LINKS)) {
                return ancestor;
            }
            ancestor = ancestor.getParent();
        }
        // The root itself is the last resort; it must exist for a create to be possible.
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
            throw new WorkspaceUnavailableException();
        }
        return root;
    }

    private void requireInsideRealRoot(Path root, Path path) {
        try {
            Path realRoot = root.toRealPath();
            Path realPath = path.toRealPath();
            if (!realPath.startsWith(realRoot)) {
                throw new WorkspaceAccessDeniedException();
            }
        } catch (IOException exception) {
            throw new WorkspaceUnavailableException();
        }
    }
}
