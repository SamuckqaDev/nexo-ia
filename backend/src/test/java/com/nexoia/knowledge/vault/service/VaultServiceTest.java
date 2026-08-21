package com.nexoia.knowledge.vault.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.audit.service.AuditService;
import com.nexoia.knowledge.vault.dto.CreateVaultRequest;
import com.nexoia.knowledge.vault.exception.UnsupportedVaultScopeException;
import com.nexoia.knowledge.vault.exception.VaultNotFoundException;
import com.nexoia.knowledge.vault.exception.VaultScopeTargetNotFoundException;
import com.nexoia.knowledge.vault.model.KnowledgeVault;
import com.nexoia.knowledge.vault.model.VaultScope;
import com.nexoia.knowledge.vault.repository.VaultRepository;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.repository.WorkspaceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VaultServiceTest {

    @Mock
    private VaultRepository vaults;
    @Mock
    private WorkspaceRepository workspaces;
    @Mock
    private AuditService audit;
    private VaultService service;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID vaultId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new VaultService(vaults, workspaces, audit);
    }

    @Test
    void createsAPersonalVaultOwnedByTheAuthenticatedUser() {
        when(vaults.save(any(KnowledgeVault.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.create(ownerId, new CreateVaultRequest("Notes", null, VaultScope.PERSONAL, null));

        assertThat(response.scope()).isEqualTo(VaultScope.PERSONAL);
        assertThat(response.workspaceId()).isNull();
    }

    @Test
    void rejectsScopesWithoutABackingAuthorizationTarget() {
        assertThatThrownBy(() -> service.create(
                ownerId, new CreateVaultRequest("Team notes", null, VaultScope.TEAM, null)))
                .isInstanceOf(UnsupportedVaultScopeException.class);

        verify(vaults, never()).save(any(KnowledgeVault.class));
    }

    @Test
    void rejectsAWorkspaceScopeTargetNotOwnedByTheCaller() {
        UUID foreignWorkspaceId = UUID.randomUUID();
        when(workspaces.findByIdAndOwnerId(foreignWorkspaceId, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(ownerId,
                new CreateVaultRequest("Project vault", null, VaultScope.WORKSPACE, foreignWorkspaceId)))
                .isInstanceOf(VaultScopeTargetNotFoundException.class);

        verify(vaults, never()).save(any(KnowledgeVault.class));
    }

    @Test
    void createsAWorkspaceScopedVaultAgainstAnOwnedWorkspace() {
        UUID workspaceId = UUID.randomUUID();
        when(workspaces.findByIdAndOwnerId(workspaceId, ownerId)).thenReturn(Optional.of(
                Workspace.builder().id(workspaceId).ownerId(ownerId).name("Acme").build()));
        when(vaults.save(any(KnowledgeVault.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.create(
                ownerId, new CreateVaultRequest("Project vault", null, VaultScope.WORKSPACE, workspaceId));

        assertThat(response.workspaceId()).isEqualTo(workspaceId);
    }

    @Test
    void hidesAnotherOwnersVaultFromArchive() {
        when(vaults.findByIdAndOwnerIdAndArchivedFalse(vaultId, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.archive(ownerId, vaultId))
                .isInstanceOf(VaultNotFoundException.class);
    }
}
