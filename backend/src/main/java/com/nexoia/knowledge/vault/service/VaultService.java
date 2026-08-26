package com.nexoia.knowledge.vault.service;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.knowledge.vault.dto.CreateVaultRequest;
import com.nexoia.knowledge.vault.dto.UpdateVaultRequest;
import com.nexoia.knowledge.vault.dto.VaultResponse;
import com.nexoia.knowledge.vault.exception.UnsupportedVaultScopeException;
import com.nexoia.knowledge.vault.exception.VaultNotFoundException;
import com.nexoia.knowledge.vault.exception.VaultScopeTargetNotFoundException;
import com.nexoia.knowledge.vault.model.KnowledgeVault;
import com.nexoia.knowledge.vault.model.VaultOwnerType;
import com.nexoia.knowledge.vault.model.VaultScope;
import com.nexoia.knowledge.vault.repository.VaultRepository;
import com.nexoia.team.exception.TeamNotFoundException;
import com.nexoia.team.model.Team;
import com.nexoia.team.repository.TeamRepository;
import com.nexoia.team.service.TeamMembershipService;
import com.nexoia.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VaultService {

    /**
     * Scopes retrieval and authorization can actually resolve today. Every other {@code VaultScope}
     * value is contract-complete but rejected at creation time. See D-026.
     */
    private static final Set<VaultScope> UNSUPPORTED_SCOPES =
            Set.of(VaultScope.PROJECT, VaultScope.TEAM, VaultScope.ORGANIZATION);

    private final VaultRepository vaults;
    private final WorkspaceRepository workspaces;
    private final TeamMembershipService teamMembershipService;
    private final TeamRepository teams;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<VaultResponse> list(UUID ownerId) {
        // The user's own Vaults plus every shared Vault owned by a Team they belong to.
        List<KnowledgeVault> visibleVaults = vaults.findAllByOwnerIdInAndArchivedFalseOrderByUpdatedAtDesc(
                teamMembershipService.accessibleOwnerIds(ownerId));
        Map<UUID, Team> teamsById = teams.findAllById(visibleVaults.stream()
                        .filter(vault -> vault.getOwnerType() == VaultOwnerType.TEAM)
                        .map(KnowledgeVault::getOwnerId)
                        .distinct()
                        .toList()).stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));
        return visibleVaults.stream()
                .map(vault -> response(vault, ownerId, teamsById))
                .toList();
    }

    @Transactional
    public VaultResponse create(UUID ownerId, CreateVaultRequest request) {
        UUID workspaceId = resolveWorkspaceId(ownerId, request.scope(), request.workspaceId());

        KnowledgeVault vault = vaults.saveAndFlush(KnowledgeVault.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .ownerType(VaultOwnerType.USER)
                .name(request.name().trim())
                .description(blankToNull(request.description()))
                .scope(request.scope())
                .workspaceId(workspaceId)
                .archived(false)
                .build());
        audit.record(RecordAuditCommand.success(
                AuditAction.VAULT_CREATED, ownerId, null, AuditTargetType.KNOWLEDGE_VAULT, vault.getId()));

        return response(vault, ownerId, Map.of());
    }

    /**
     * Creates a Team-owned Vault: shared knowledge for that Team's members. The Team service authorizes
     * the actor first, including the system Owner fast-path. Team Vaults
     * use PERSONAL scope (a plain shared corpus, no workspace binding).
     */
    @Transactional
    public VaultResponse createForTeam(UUID actorId, UUID teamId, String name, String description) {
        Team team = teams.findById(teamId).orElseThrow(TeamNotFoundException::new);
        KnowledgeVault vault = vaults.saveAndFlush(KnowledgeVault.builder()
                .id(UUID.randomUUID())
                .ownerId(teamId)
                .ownerType(VaultOwnerType.TEAM)
                .name(name.trim())
                .description(blankToNull(description))
                .scope(VaultScope.PERSONAL)
                .archived(false)
                .build());
        audit.record(RecordAuditCommand.success(
                AuditAction.VAULT_CREATED, actorId, null, AuditTargetType.KNOWLEDGE_VAULT, vault.getId()));

        return response(vault, actorId, Map.of(teamId, team));
    }

    @Transactional
    public VaultResponse update(UUID ownerId, UUID vaultId, UpdateVaultRequest request) {
        KnowledgeVault vault = manageableVault(ownerId, vaultId);
        vault.update(request.name().trim(), blankToNull(request.description()));

        return response(vault, ownerId, teamMap(vault));
    }

    /**
     * Toggles whether the assistant's governed {@code save_to_vault} tool may append knowledge to this
     * owned Vault. Read-only by default; the owner opts a Vault in explicitly.
     */
    @Transactional
    public VaultResponse setWritable(UUID ownerId, UUID vaultId, boolean writable) {
        KnowledgeVault vault = manageableVault(ownerId, vaultId);
        vault.applyWritable(writable);
        audit.record(RecordAuditCommand.success(
                writable ? AuditAction.VAULT_WRITE_ENABLED : AuditAction.VAULT_WRITE_DISABLED,
                ownerId, null, AuditTargetType.KNOWLEDGE_VAULT, vaultId));

        return response(vault, ownerId, teamMap(vault));
    }

    @Transactional
    public void archive(UUID ownerId, UUID vaultId) {
        manageableVault(ownerId, vaultId).archive();
        audit.record(RecordAuditCommand.success(
                AuditAction.VAULT_ARCHIVED, ownerId, null, AuditTargetType.KNOWLEDGE_VAULT, vaultId));
    }

    /**
     * Resolves a Vault visible to the caller. Used by ingestion and retrieval so every downstream
     * lookup shares the same 404-on-not-authorized isolation boundary.
     */
    @Transactional(readOnly = true)
    public KnowledgeVault accessibleVault(UUID userId, UUID vaultId) {
        return vaults.findByIdAndOwnerIdInAndArchivedFalse(
                        vaultId, teamMembershipService.accessibleOwnerIds(userId))
                .orElseThrow(VaultNotFoundException::new);
    }

    /** Resolves a Vault the user may change: their own, or a Team Vault they administer. */
    @Transactional(readOnly = true)
    public KnowledgeVault manageableVault(UUID userId, UUID vaultId) {
        KnowledgeVault vault = accessibleVault(userId, vaultId);
        boolean manageable = vault.getOwnerType() == VaultOwnerType.USER
                ? vault.getOwnerId().equals(userId)
                : teamMembershipService.canManage(userId, vault.getOwnerId());
        if (!manageable) {
            throw new VaultNotFoundException();
        }
        return vault;
    }

    private UUID resolveWorkspaceId(UUID ownerId, VaultScope scope, UUID requestedWorkspaceId) {
        if (UNSUPPORTED_SCOPES.contains(scope)) {
            throw new UnsupportedVaultScopeException();
        }
        if (scope != VaultScope.WORKSPACE) {
            return null;
        }
        if (requestedWorkspaceId == null) {
            throw new VaultScopeTargetNotFoundException();
        }

        return workspaces.findByIdAndOwnerId(requestedWorkspaceId, ownerId)
                .orElseThrow(VaultScopeTargetNotFoundException::new)
                .getId();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<UUID, Team> teamMap(KnowledgeVault vault) {
        if (vault.getOwnerType() != VaultOwnerType.TEAM) {
            return Map.of();
        }
        Team team = teams.findById(vault.getOwnerId()).orElseThrow(TeamNotFoundException::new);
        return Map.of(team.getId(), team);
    }

    private VaultResponse response(KnowledgeVault value, UUID userId, Map<UUID, Team> teamsById) {
        boolean manageable = value.getOwnerType() == VaultOwnerType.USER
                ? value.getOwnerId().equals(userId)
                : teamMembershipService.canManage(userId, value.getOwnerId());
        String ownerName = value.getOwnerType() == VaultOwnerType.USER
                ? "Personal space"
                : teamName(value.getOwnerId(), teamsById);
        return new VaultResponse(
                value.getId(),
                value.getName(),
                value.getDescription(),
                value.getScope(),
                value.getWorkspaceId(),
                value.getOwnerId(),
                value.getOwnerType(),
                ownerName,
                manageable,
                value.isWritable(),
                value.getCreatedAt(),
                value.getUpdatedAt());
    }

    private String teamName(UUID teamId, Map<UUID, Team> teamsById) {
        Team team = teamsById.get(teamId);
        if (team == null) {
            throw new TeamNotFoundException();
        }
        return team.getName();
    }
}
