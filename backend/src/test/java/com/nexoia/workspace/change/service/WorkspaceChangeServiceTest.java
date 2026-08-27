package com.nexoia.workspace.change.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nexoia.conversation.chat.repository.ConversationRepository;
import com.nexoia.provider.dto.WorkspaceToolScope;
import com.nexoia.workspace.change.dto.WorkspaceChangeResponse;
import com.nexoia.workspace.change.exception.WorkspaceChangeStateException;
import com.nexoia.workspace.change.model.WorkspaceChangeArtifact;
import com.nexoia.workspace.change.model.WorkspaceChangeStatus;
import com.nexoia.workspace.change.repository.WorkspaceChangeArtifactRepository;
import com.nexoia.workspace.config.WorkspaceProperties;
import com.nexoia.workspace.config.WorkspaceStorage;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.model.WorkspaceAccessMode;
import com.nexoia.workspace.model.WorkspaceStorageType;
import com.nexoia.workspace.service.WorkspaceAccessService;
import com.nexoia.workspace.service.WorkspaceBindingService;
import com.nexoia.workspace.service.WorkspaceContentPolicy;
import com.nexoia.workspace.service.WorkspacePathResolver;
import com.nexoia.workspace.tool.LocalWorkspaceToolGateway;
import com.nexoia.workspace.tool.WorkspaceApplyPatchInput;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceChangeServiceTest {

    @TempDir Path temporaryDirectory;

    private WorkspaceChangeArtifactRepository changes;
    private WorkspaceChangeService service;
    private WorkspaceToolScope scope;
    private Path target;

    @BeforeEach
    void setUp() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        Workspace workspace = Workspace.builder()
                .id(workspaceId)
                .ownerId(userId)
                .name("Nexo")
                .storageType(WorkspaceStorageType.MANAGED)
                .accessMode(WorkspaceAccessMode.WRITE_WITH_APPROVAL)
                .build();
        scope = new WorkspaceToolScope(
                userId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                workspaceId,
                "Nexo",
                WorkspaceAccessMode.WRITE_WITH_APPROVAL,
                true,
                null,
                null,
                null,
                true);
        WorkspaceProperties properties = new WorkspaceProperties(
                temporaryDirectory.resolve("workspaces").toString(),
                "",
                temporaryDirectory.resolve("artifacts").toString(),
                1_048_576L,
                500,
                100,
                Duration.ofMinutes(15),
                Duration.ofMinutes(10));
        Path root = properties.managedRootPath()
                .resolve(userId.toString())
                .resolve(workspaceId.toString());
        Files.createDirectories(root);
        target = root.resolve("README.md");
        Files.writeString(target, "# Nexo\n\nold value\n");

        changes = mock(WorkspaceChangeArtifactRepository.class);
        AtomicReference<WorkspaceChangeArtifact> saved = new AtomicReference<>();
        when(changes.saveAndFlush(any())).thenAnswer(invocation -> {
            WorkspaceChangeArtifact change = invocation.getArgument(0);
            saved.set(change);
            return change;
        });
        when(changes.findByIdAndUserId(any(), any()))
                .thenAnswer(ignored -> Optional.ofNullable(saved.get()));
        WorkspaceAccessService access = mock(WorkspaceAccessService.class);
        when(access.accessibleWorkspace(userId, workspaceId)).thenReturn(workspace);
        WorkspaceStorage storage = mock(WorkspaceStorage.class);
        when(storage.isArtifactRootAvailable()).thenReturn(true);
        service = new WorkspaceChangeService(
                changes,
                mock(ConversationRepository.class),
                access,
                mock(WorkspaceBindingService.class),
                new WorkspacePathResolver(properties),
                new WorkspaceContentPolicy(),
                properties,
                new WorkspaceChangeArtifactStore(properties, storage),
                mock(LocalWorkspaceToolGateway.class),
                Clock.fixed(Instant.parse("2026-08-27T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void previewsAppliesAndRevertsAnExactServerSideEdit() throws Exception {
        var proposal = service.proposePatch(
                scope,
                new WorkspaceApplyPatchInput("README.md", "old value", "new value", false));

        assertThat(Files.readString(target)).contains("old value");
        assertThat(proposal.approvalRequired()).isTrue();
        assertThat(proposal.replacementCount()).isEqualTo(1);

        WorkspaceChangeResponse applied = service.approve(scope.userId(), proposal.changeId());

        assertThat(applied.status()).isEqualTo(WorkspaceChangeStatus.APPLIED);
        assertThat(Files.readString(target)).contains("new value").doesNotContain("old value");

        WorkspaceChangeResponse reverted = service.revert(scope.userId(), proposal.changeId());

        assertThat(reverted.status()).isEqualTo(WorkspaceChangeStatus.REVERTED);
        assertThat(Files.readString(target)).contains("old value").doesNotContain("new value");
    }

    @Test
    void invalidatesApprovalWhenTheFileChangedAfterThePreview() throws Exception {
        var proposal = service.proposePatch(
                scope,
                new WorkspaceApplyPatchInput("README.md", "old value", "new value", false));
        Files.writeString(target, "# Nexo\n\nexternal change\n");

        WorkspaceChangeResponse result = service.approve(scope.userId(), proposal.changeId());

        assertThat(result.status()).isEqualTo(WorkspaceChangeStatus.INVALIDATED);
        assertThat(result.failureCode()).isEqualTo("WORKSPACE_CHANGED");
        assertThat(Files.readString(target)).contains("external change").doesNotContain("new value");
    }

    @Test
    void refusesAnAmbiguousPatchInsteadOfGuessingWhichOccurrenceToEdit() throws Exception {
        Files.writeString(target, "old value\nold value\n");

        assertThatThrownBy(() -> service.proposePatch(
                scope,
                new WorkspaceApplyPatchInput("README.md", "old value", "new value", false)))
                .isInstanceOf(WorkspaceChangeStateException.class)
                .hasMessageContaining("multiple locations");
    }
}
