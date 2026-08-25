package com.nexoia.team.service;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.user.exception.UserNotFoundException;
import com.nexoia.auth.user.model.UserAccount;
import com.nexoia.auth.user.model.UserRole;
import com.nexoia.auth.user.repository.UserAccountRepository;
import com.nexoia.permission.model.ProfileKey;
import com.nexoia.permission.service.PermissionAdminService;
import com.nexoia.team.dto.AddTeamMemberRequest;
import com.nexoia.team.dto.CreateTeamRequest;
import com.nexoia.team.dto.CreateTeamVaultRequest;
import com.nexoia.team.dto.TeamMemberResponse;
import com.nexoia.team.dto.TeamResponse;
import com.nexoia.team.exception.TeamAdministrationDeniedException;
import com.nexoia.team.exception.TeamMembershipConflictException;
import com.nexoia.team.exception.TeamNotFoundException;
import com.nexoia.team.model.Team;
import com.nexoia.team.model.TeamMembership;
import com.nexoia.team.model.TeamRole;
import com.nexoia.team.repository.TeamMembershipRepository;
import com.nexoia.team.repository.TeamRepository;
import com.nexoia.knowledge.vault.dto.VaultResponse;
import com.nexoia.knowledge.vault.service.VaultService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates Teams and administers their membership under the governance model. Every grant passes through
 * the delegation invariants ({@link PermissionAdminService}): a Team admin may only assign a profile at
 * or below their own ceiling, and only a system OWNER may appoint a Team admin. See
 * {@code docs/ORGANIZATIONS_AND_GOVERNANCE.md} section 4.
 */
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teams;
    private final TeamMembershipRepository memberships;
    private final UserAccountRepository users;
    private final PermissionAdminService permissionAdminService;
    private final VaultService vaultService;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<TeamResponse> listMyTeams(UUID userId) {
        return memberships.findAllByUserIdOrderByJoinedAtAsc(userId).stream()
                .map(membership -> teams.findById(membership.getTeamId()).orElseThrow(TeamNotFoundException::new))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> listMembers(NexoUserPrincipal principal, UUID teamId) {
        requireTeamAdmin(principal, teamId);
        return memberships.findAllByTeamIdOrderByJoinedAtAsc(teamId).stream()
                .map(membership -> new TeamMemberResponse(membership.getUserId(), membership.getTeamRole(),
                        membership.getAssignedProfile(), membership.getJoinedAt()))
                .toList();
    }

    @Transactional
    public TeamResponse createTeam(NexoUserPrincipal principal, CreateTeamRequest request) {
        ProfileKey defaultProfile =
                request.defaultProfile() == null ? ProfileKey.RESEARCHER : request.defaultProfile();
        Team team = teams.saveAndFlush(Team.builder()
                .id(UUID.randomUUID())
                .name(request.name().trim())
                .createdBy(principal.userId())
                .tokenBudgetLimit(request.tokenBudgetLimit())
                .defaultProfile(defaultProfile)
                .build());

        // The creator becomes the Team's admin, carrying their own system profile inside the Team.
        memberships.saveAndFlush(TeamMembership.builder()
                .id(UUID.randomUUID())
                .teamId(team.getId())
                .userId(principal.userId())
                .teamRole(TeamRole.ADMIN)
                .assignedProfile(actorProfile(principal.userId()))
                .build());

        audit.record(RecordAuditCommand.success(AuditAction.TEAM_CREATED, principal.userId(),
                principal.role(), AuditTargetType.TEAM, team.getId()));
        return toResponse(team);
    }

    @Transactional
    public TeamMemberResponse addMember(NexoUserPrincipal principal, UUID teamId, AddTeamMemberRequest request) {
        Team team = requireTeamAdmin(principal, teamId);
        UserAccount target = users.findById(request.userId()).orElseThrow(UserNotFoundException::new);
        if (memberships.existsByTeamIdAndUserId(teamId, target.getId())) {
            throw new TeamMembershipConflictException();
        }

        TeamRole teamRole = request.teamRole() == null ? TeamRole.MEMBER : request.teamRole();
        // Only a system OWNER may appoint a Team admin; a Team admin adds members.
        if (teamRole == TeamRole.ADMIN && principal.role() != UserRole.OWNER) {
            throw new TeamAdministrationDeniedException();
        }

        ProfileKey assignedProfile = request.profile() == null ? team.getDefaultProfile() : request.profile();
        // The profile granted may never exceed the actor's own ceiling (OWNER is unbounded).
        permissionAdminService.assertCanGrant(
                principal.role(), actorProfile(principal.userId()), UserRole.MEMBER, assignedProfile);

        TeamMembership membership = memberships.saveAndFlush(TeamMembership.builder()
                .id(UUID.randomUUID())
                .teamId(teamId)
                .userId(target.getId())
                .teamRole(teamRole)
                .assignedProfile(assignedProfile)
                .build());
        audit.record(RecordAuditCommand.success(AuditAction.TEAM_MEMBER_ADDED, principal.userId(),
                principal.role(), AuditTargetType.TEAM, teamId));
        return new TeamMemberResponse(membership.getUserId(), membership.getTeamRole(),
                membership.getAssignedProfile(), membership.getJoinedAt());
    }

    @Transactional
    public VaultResponse createVault(
            NexoUserPrincipal principal, UUID teamId,
            CreateTeamVaultRequest request) {
        requireTeamAdmin(principal, teamId);
        return vaultService.createForTeam(principal.userId(), teamId, request.name(), request.description());
    }

    /**
     * Resolves a Team the caller may administer: a system OWNER administers any Team, otherwise the caller
     * must be a Team admin of it. A Team that is not visible answers 404, not 403.
     */
    private Team requireTeamAdmin(NexoUserPrincipal principal, UUID teamId) {
        Team team = teams.findById(teamId).orElseThrow(TeamNotFoundException::new);
        if (principal.role() != UserRole.OWNER
                && !memberships.existsByTeamIdAndUserIdAndTeamRole(teamId, principal.userId(), TeamRole.ADMIN)) {
            throw new TeamAdministrationDeniedException();
        }
        return team;
    }

    private ProfileKey actorProfile(UUID userId) {
        return users.findById(userId).orElseThrow(UserNotFoundException::new).getAssignedProfile();
    }

    private TeamResponse toResponse(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getCreatedBy(),
                team.getDefaultProfile(), team.getTokenBudgetLimit(), team.getCreatedAt(), team.getUpdatedAt());
    }
}
