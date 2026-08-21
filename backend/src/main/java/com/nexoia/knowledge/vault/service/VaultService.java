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
import com.nexoia.knowledge.vault.model.VaultScope;
import com.nexoia.knowledge.vault.repository.VaultRepository;
import com.nexoia.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<VaultResponse> list(UUID ownerId) {
        return vaults.findAllByOwnerIdAndArchivedFalseOrderByUpdatedAtDesc(ownerId).stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public VaultResponse create(UUID ownerId, CreateVaultRequest request) {
        UUID workspaceId = resolveWorkspaceId(ownerId, request.scope(), request.workspaceId());

        KnowledgeVault vault = vaults.save(KnowledgeVault.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .name(request.name().trim())
                .description(blankToNull(request.description()))
                .scope(request.scope())
                .workspaceId(workspaceId)
                .archived(false)
                .build());
        audit.record(RecordAuditCommand.success(
                AuditAction.VAULT_CREATED, ownerId, null, AuditTargetType.KNOWLEDGE_VAULT, vault.getId()));

        return response(vault);
    }

    @Transactional
    public VaultResponse update(UUID ownerId, UUID vaultId, UpdateVaultRequest request) {
        KnowledgeVault vault = ownedVault(ownerId, vaultId);
        vault.update(request.name().trim(), blankToNull(request.description()));

        return response(vault);
    }

    @Transactional
    public void archive(UUID ownerId, UUID vaultId) {
        ownedVault(ownerId, vaultId).archive();
        audit.record(RecordAuditCommand.success(
                AuditAction.VAULT_ARCHIVED, ownerId, null, AuditTargetType.KNOWLEDGE_VAULT, vaultId));
    }

    /**
     * Resolves a vault owned by the caller. Used by the ingestion and retrieval services too, so
     * every downstream lookup shares the same 404-on-not-owned isolation boundary.
     */
    @Transactional(readOnly = true)
    public KnowledgeVault ownedVault(UUID ownerId, UUID vaultId) {
        return vaults.findByIdAndOwnerIdAndArchivedFalse(vaultId, ownerId)
                .orElseThrow(VaultNotFoundException::new);
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

    private VaultResponse response(KnowledgeVault value) {
        return new VaultResponse(
                value.getId(),
                value.getName(),
                value.getDescription(),
                value.getScope(),
                value.getWorkspaceId(),
                value.getCreatedAt(),
                value.getUpdatedAt());
    }
}
