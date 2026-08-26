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
import com.nexoia.knowledge.vault.model.VaultOwnerType;
import com.nexoia.knowledge.vault.model.VaultScope;
import com.nexoia.knowledge.vault.repository.VaultRepository;
import com.nexoia.team.model.Team;
import com.nexoia.team.repository.TeamRepository;
import com.nexoia.team.service.TeamMembershipService;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.repository.WorkspaceRepository;
import java.time.Instant;
import java.util.List;
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
    private TeamMembershipService teamMembershipService;
    @Mock
    private TeamRepository teams;
    @Mock
    private AuditService audit;
    private VaultService service;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID vaultId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new VaultService(vaults, workspaces, teamMembershipService, teams, audit);
    }

    @Test
    void createsAPersonalVaultOwnedByTheAuthenticatedUser() {
        when(vaults.saveAndFlush(any(KnowledgeVault.class)))
                .thenAnswer(call -> persistedVault(call.getArgument(0)));

        var response = service.create(ownerId, new CreateVaultRequest("Notes", null, VaultScope.PERSONAL, null));

        assertThat(response.scope()).isEqualTo(VaultScope.PERSONAL);
        assertThat(response.ownerId()).isEqualTo(ownerId);
        assertThat(response.ownerType()).isEqualTo(VaultOwnerType.USER);
        assertThat(response.ownerName()).isEqualTo("Personal space");
        assertThat(response.manageable()).isTrue();
        assertThat(response.workspaceId()).isNull();
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void rejectsScopesWithoutABackingAuthorizationTarget() {
        assertThatThrownBy(() -> service.create(
                ownerId, new CreateVaultRequest("Team notes", null, VaultScope.TEAM, null)))
                .isInstanceOf(UnsupportedVaultScopeException.class);

        verify(vaults, never()).saveAndFlush(any(KnowledgeVault.class));
    }

    @Test
    void rejectsAWorkspaceScopeTargetNotOwnedByTheCaller() {
        UUID foreignWorkspaceId = UUID.randomUUID();
        when(workspaces.findByIdAndOwnerId(foreignWorkspaceId, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(ownerId,
                new CreateVaultRequest("Project vault", null, VaultScope.WORKSPACE, foreignWorkspaceId)))
                .isInstanceOf(VaultScopeTargetNotFoundException.class);

        verify(vaults, never()).saveAndFlush(any(KnowledgeVault.class));
    }

    @Test
    void createsAWorkspaceScopedVaultAgainstAnOwnedWorkspace() {
        UUID workspaceId = UUID.randomUUID();
        when(workspaces.findByIdAndOwnerId(workspaceId, ownerId)).thenReturn(Optional.of(
                Workspace.builder().id(workspaceId).ownerId(ownerId).name("Acme").build()));
        when(vaults.saveAndFlush(any(KnowledgeVault.class)))
                .thenAnswer(call -> persistedVault(call.getArgument(0)));

        var response = service.create(
                ownerId, new CreateVaultRequest("Project vault", null, VaultScope.WORKSPACE, workspaceId));

        assertThat(response.workspaceId()).isEqualTo(workspaceId);
    }

    @Test
    void flushesATeamVaultBeforeBuildingItsTimestampedResponse() {
        UUID teamId = UUID.randomUUID();
        when(teams.findById(teamId)).thenReturn(Optional.of(
                Team.builder().id(teamId).name("Platform").createdBy(ownerId).build()));
        when(teamMembershipService.canManage(ownerId, teamId)).thenReturn(true);
        when(vaults.saveAndFlush(any(KnowledgeVault.class)))
                .thenAnswer(call -> persistedVault(call.getArgument(0)));

        var response = service.createForTeam(ownerId, teamId, "Shared notes", null);

        assertThat(response.name()).isEqualTo("Shared notes");
        assertThat(response.ownerType()).isEqualTo(VaultOwnerType.TEAM);
        assertThat(response.ownerName()).isEqualTo("Platform");
        assertThat(response.manageable()).isTrue();
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    private KnowledgeVault persistedVault(KnowledgeVault vault) {
        Instant now = Instant.parse("2026-08-25T00:00:00Z");
        return KnowledgeVault.builder()
                .id(vault.getId())
                .ownerId(vault.getOwnerId())
                .ownerType(vault.getOwnerType())
                .name(vault.getName())
                .description(vault.getDescription())
                .scope(vault.getScope())
                .workspaceId(vault.getWorkspaceId())
                .archived(vault.isArchived())
                .writable(vault.isWritable())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    void hidesAnotherOwnersVaultFromArchive() {
        when(teamMembershipService.accessibleOwnerIds(ownerId)).thenReturn(List.of(ownerId));
        when(vaults.findByIdAndOwnerIdInAndArchivedFalse(vaultId, List.of(ownerId)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.archive(ownerId, vaultId))
                .isInstanceOf(VaultNotFoundException.class);
    }
}
