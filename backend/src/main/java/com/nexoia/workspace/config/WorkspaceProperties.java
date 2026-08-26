package com.nexoia.workspace.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Server-side Workspace execution limits and storage roots.
 *
 * <p>Only the Nexo server resolves these paths; the model and the browser never see them. An empty
 * {@code importRoot} means registering a {@code MOUNTED} Workspace is unavailable — there is
 * deliberately no fallback to {@code /}, {@code $HOME} or the working directory.
 */
@ConfigurationProperties(prefix = "nexo.workspace")
public record WorkspaceProperties(
        @DefaultValue(".nexo-data/workspaces") String managedRoot,
        @DefaultValue("") String importRoot,
        @DefaultValue(".nexo-data/artifacts") String artifactRoot,
        @DefaultValue("1048576") long maxFileBytes,
        @DefaultValue("500") int maxTreeEntries,
        @DefaultValue("100") int maxSearchMatches,
        @DefaultValue("15m") Duration approvalTimeout,
        @DefaultValue("10m") Duration commandTimeout) {

    public Path managedRootPath() {
        return normalize(managedRoot);
    }

    public Path artifactRootPath() {
        return normalize(artifactRoot);
    }

    /** Present only when an import root is configured; absent disables {@code MOUNTED} registration. */
    public Optional<Path> importRootPath() {
        return hasImportRoot() ? Optional.of(normalize(importRoot)) : Optional.empty();
    }

    public boolean hasImportRoot() {
        return importRoot != null && !importRoot.isBlank();
    }

    private Path normalize(String value) {
        return Path.of(value).toAbsolutePath().normalize();
    }
}
