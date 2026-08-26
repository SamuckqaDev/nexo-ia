package com.nexoia.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import com.nexoia.workspace.config.WorkspaceProperties;
import com.nexoia.workspace.exception.WorkspaceNotFoundException;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.model.WorkspaceAccessMode;
import com.nexoia.workspace.model.WorkspaceStatus;
import com.nexoia.workspace.model.WorkspaceStorageType;
import com.nexoia.workspace.repository.WorkspaceRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceAccessServiceTest {

    private static final UUID USER = UUID.randomUUID();

    @Mock
    private WorkspaceRepository workspaces;

    @TempDir
    Path importRoot;

    @TempDir
    Path managed;

    private WorkspaceAccessService access;

    @BeforeEach
    void setUp() {
        WorkspaceProperties properties = new WorkspaceProperties(
                managed.toString(), importRoot.toString(), managed.resolve("artifacts").toString(),
                1_048_576, 500, 100, Duration.ofMinutes(15), Duration.ofMinutes(10));
        access = new WorkspaceAccessService(workspaces, new WorkspacePathResolver(properties));
    }

    private Workspace mounted(String relativePath) {
        return Workspace.builder()
                .id(UUID.randomUUID()).ownerId(USER).name("proj")
                .storageType(WorkspaceStorageType.MOUNTED)
                .accessMode(WorkspaceAccessMode.READ_ONLY)
                .relativePath(relativePath)
                .build();
    }

    @Test
    void resolvesOwnedWorkspace() {
        Workspace workspace = mounted("project");
        when(workspaces.findByIdAndOwnerId(workspace.getId(), USER)).thenReturn(Optional.of(workspace));

        assertThat(access.accessibleWorkspace(USER, workspace.getId())).isSameAs(workspace);
    }

    @Test
    void hidesForeignWorkspaceAsNotFound() {
        UUID id = UUID.randomUUID();
        when(workspaces.findByIdAndOwnerId(id, USER)).thenReturn(Optional.empty());

        assertThatExceptionOfType(WorkspaceNotFoundException.class)
                .isThrownBy(() -> access.accessibleWorkspace(USER, id));
    }

    @Test
    void reportsUnboundStatus() {
        Workspace unbound = Workspace.builder().id(UUID.randomUUID()).ownerId(USER).name("u").build();

        assertThat(access.lightStatus(unbound)).isEqualTo(WorkspaceStatus.UNBOUND);
    }

    @Test
    void reportsAvailableWhenDirectoryExists() throws IOException {
        Files.createDirectories(importRoot.resolve("project"));

        assertThat(access.lightStatus(mounted("project"))).isEqualTo(WorkspaceStatus.AVAILABLE);
    }

    @Test
    void reportsMissingWhenDirectoryAbsent() {
        assertThat(access.lightStatus(mounted("absent"))).isEqualTo(WorkspaceStatus.MISSING);
    }
}
