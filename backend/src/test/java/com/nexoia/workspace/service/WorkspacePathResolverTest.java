package com.nexoia.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.nexoia.workspace.config.WorkspaceProperties;
import com.nexoia.workspace.exception.WorkspaceAccessDeniedException;
import com.nexoia.workspace.exception.WorkspaceInvalidPathException;
import com.nexoia.workspace.exception.WorkspaceNotBoundException;
import com.nexoia.workspace.exception.WorkspaceUnavailableException;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.model.WorkspaceAccessMode;
import com.nexoia.workspace.model.WorkspaceStorageType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspacePathResolverTest {

    private static final UUID OWNER = UUID.randomUUID();

    private WorkspaceProperties properties(Path managed, Path importRoot) {
        return new WorkspaceProperties(
                managed.toString(), importRoot == null ? "" : importRoot.toString(),
                managed.resolve("artifacts").toString(), 1_048_576, 500, 100,
                Duration.ofMinutes(15), Duration.ofMinutes(10));
    }

    private Workspace mounted(String relativePath) {
        return Workspace.builder()
                .id(UUID.randomUUID())
                .ownerId(OWNER)
                .name("proj")
                .storageType(WorkspaceStorageType.MOUNTED)
                .accessMode(WorkspaceAccessMode.READ_ONLY)
                .relativePath(relativePath)
                .build();
    }

    @Test
    void resolvesManagedRootFromOwnerAndId(@TempDir Path managed) {
        WorkspacePathResolver resolver = new WorkspacePathResolver(properties(managed, null));
        Workspace workspace = Workspace.builder()
                .id(UUID.randomUUID()).ownerId(OWNER).name("m")
                .storageType(WorkspaceStorageType.MANAGED)
                .accessMode(WorkspaceAccessMode.READ_ONLY)
                .build();

        Path root = resolver.workspaceRoot(workspace);

        assertThat(root).isEqualTo(managed.toAbsolutePath().normalize()
                .resolve(OWNER.toString()).resolve(workspace.getId().toString()));
    }

    @Test
    void resolvesMountedRootUnderImportRoot(@TempDir Path managed, @TempDir Path importRoot) throws IOException {
        Files.createDirectories(importRoot.resolve("project"));
        WorkspacePathResolver resolver = new WorkspacePathResolver(properties(managed, importRoot));

        Path root = resolver.workspaceRoot(mounted("project"));

        assertThat(root).isEqualTo(importRoot.toAbsolutePath().normalize().resolve("project"));
    }

    @Test
    void rejectsUnboundWorkspace(@TempDir Path managed) {
        WorkspacePathResolver resolver = new WorkspacePathResolver(properties(managed, null));
        Workspace unbound = Workspace.builder().id(UUID.randomUUID()).ownerId(OWNER).name("u").build();

        assertThatExceptionOfType(WorkspaceNotBoundException.class)
                .isThrownBy(() -> resolver.workspaceRoot(unbound));
    }

    @Test
    void rejectsMountedWhenImportRootMissing(@TempDir Path managed) {
        WorkspacePathResolver resolver = new WorkspacePathResolver(properties(managed, null));

        assertThatExceptionOfType(WorkspaceUnavailableException.class)
                .isThrownBy(() -> resolver.workspaceRoot(mounted("project")));
    }

    @Test
    void rejectsTraversalAbsoluteAndDangerousSegments(@TempDir Path managed, @TempDir Path importRoot)
            throws IOException {
        Files.createDirectories(importRoot.resolve("project/src"));
        WorkspacePathResolver resolver = new WorkspacePathResolver(properties(managed, importRoot));
        Workspace workspace = mounted("project");

        for (String bad : new String[] {"../secret", "src/../../escape", "/etc/passwd",
                "src/\u0000nul", "~/home", "C:\\win", "src\\win", "a//b", "..", "src/.."}) {
            assertThatExceptionOfType(RuntimeException.class)
                    .as("path %s", bad)
                    .isThrownBy(() -> resolver.resolveExisting(workspace, bad))
                    .matches(exception -> exception instanceof WorkspaceInvalidPathException
                            || exception instanceof WorkspaceAccessDeniedException);
        }
    }

    @Test
    void resolvesExistingFileInsideRoot(@TempDir Path managed, @TempDir Path importRoot) throws IOException {
        Files.createDirectories(importRoot.resolve("project/src"));
        Files.writeString(importRoot.resolve("project/src/App.java"), "class App {}");
        WorkspacePathResolver resolver = new WorkspacePathResolver(properties(managed, importRoot));

        Path resolved = resolver.resolveExisting(mounted("project"), "src/App.java");

        assertThat(resolved).isEqualTo(importRoot.toAbsolutePath().normalize().resolve("project/src/App.java"));
    }

    @Test
    void rejectsSymlinkEscapingRoot(@TempDir Path managed, @TempDir Path importRoot, @TempDir Path outside)
            throws IOException {
        Files.createDirectories(importRoot.resolve("project"));
        Files.writeString(outside.resolve("secret.txt"), "top secret");
        Path link = importRoot.resolve("project/leak.txt");
        Files.createSymbolicLink(link, outside.resolve("secret.txt"));
        WorkspacePathResolver resolver = new WorkspacePathResolver(properties(managed, importRoot));

        assertThatExceptionOfType(WorkspaceAccessDeniedException.class)
                .isThrownBy(() -> resolver.resolveExisting(mounted("project"), "leak.txt"));
    }

    @Test
    void allowsCreateForNewFileWithExistingAncestor(@TempDir Path managed, @TempDir Path importRoot)
            throws IOException {
        Files.createDirectories(importRoot.resolve("project/src"));
        WorkspacePathResolver resolver = new WorkspacePathResolver(properties(managed, importRoot));

        assertThatCode(() -> resolver.resolveForCreate(mounted("project"), "src/New.java"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsDeletionOfRootItself(@TempDir Path managed, @TempDir Path importRoot) throws IOException {
        Files.createDirectories(importRoot.resolve("project"));
        WorkspacePathResolver resolver = new WorkspacePathResolver(properties(managed, importRoot));
        Workspace workspace = mounted("project");
        Path root = resolver.workspaceRoot(workspace);

        assertThatExceptionOfType(WorkspaceAccessDeniedException.class)
                .isThrownBy(() -> resolver.requireDeletableTarget(workspace, root));
    }
}
