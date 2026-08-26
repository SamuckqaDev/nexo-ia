package com.nexoia.workspace.service;

import com.nexoia.workspace.exception.WorkspaceAccessDeniedException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Central policy for workspace paths that may be exposed to a model or browser. Build outputs and
 * internal directories are omitted, while credential-bearing files are denied even when they are
 * located inside an otherwise authorized workspace.
 */
@Component
public class WorkspaceContentPolicy {

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git", "node_modules", "target", "build", ".gradle",
            ".idea", ".vscode", ".nexo-runtime", "dist", "coverage",
            ".ssh", ".aws", ".azure", ".gnupg", ".kube");
    private static final Set<String> SENSITIVE_NAMES = Set.of(
            ".env", ".env.local", ".env.production", ".env.development",
            "credentials", "credentials.json", "secrets.json",
            "id_rsa", "id_ed25519", ".npmrc", ".pypirc", ".netrc");
    private static final Set<String> SENSITIVE_SUFFIXES = Set.of(
            ".pem", ".key", ".p12", ".pfx", ".jks", ".keystore");

    public boolean isIgnored(Path root, Path path) {
        Path relative = root.relativize(path);
        for (Path segment : relative) {
            if (IGNORED_DIRECTORIES.contains(segment.toString())) {
                return true;
            }
        }
        return false;
    }

    public boolean isIgnoredDirectoryName(String name) {
        return IGNORED_DIRECTORIES.contains(name);
    }

    public void requireReadable(String relativePath) {
        if (isSensitive(relativePath)) {
            throw new WorkspaceAccessDeniedException();
        }
    }

    public boolean isSensitive(String relativePath) {
        String normalized = relativePath.toLowerCase(Locale.ROOT);
        int separator = Math.max(normalized.lastIndexOf('/'), normalized.lastIndexOf('\\'));
        String fileName = separator < 0 ? normalized : normalized.substring(separator + 1);
        boolean environmentFile = fileName.startsWith(".env");
        boolean sensitiveSuffix = SENSITIVE_SUFFIXES.stream().anyMatch(fileName::endsWith);
        return environmentFile || SENSITIVE_NAMES.contains(fileName) || sensitiveSuffix;
    }
}
