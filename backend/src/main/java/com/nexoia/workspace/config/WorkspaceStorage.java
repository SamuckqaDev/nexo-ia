package com.nexoia.workspace.config;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Owns the Workspace storage roots and their startup availability. The managed and artifact roots are
 * created under the process owner at startup; a permission failure is captured as a safe status rather
 * than crashing the application, so ordinary chat keeps working while managed Workspaces stay
 * unavailable. Full paths are never logged at INFO.
 */
@Slf4j
@Component
public class WorkspaceStorage {

    private final WorkspaceProperties properties;

    @Getter
    private boolean managedRootAvailable;

    @Getter
    private boolean artifactRootAvailable;

    @Getter
    private String unavailableReason;

    public WorkspaceStorage(WorkspaceProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void prepare() {
        managedRootAvailable = ensureDirectory(properties.managedRootPath(), "managed");
        artifactRootAvailable = ensureDirectory(properties.artifactRootPath(), "artifact");
        if (!properties.hasImportRoot()) {
            log.info("Workspace import root not configured; mounted Workspaces are unavailable");
        }
    }

    /** A configured import root that actually resolves to an existing readable directory. */
    public boolean importRootAvailable() {
        return properties.importRootPath()
                .map(path -> Files.isDirectory(path) && Files.isReadable(path))
                .orElse(false);
    }

    private boolean ensureDirectory(Path path, String label) {
        try {
            Files.createDirectories(path);
            log.info("Workspace {} root ready", label);
            return true;
        } catch (IOException | SecurityException exception) {
            unavailableReason = "Workspace %s storage is not writable".formatted(label);
            log.warn("Workspace {} root could not be prepared: {}", label, exception.getClass().getSimpleName());
            return false;
        }
    }
}
