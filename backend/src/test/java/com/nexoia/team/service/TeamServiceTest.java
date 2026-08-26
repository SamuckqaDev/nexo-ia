package com.nexoia.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.audit.service.AuditService;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.user.model.UserAccount;
import com.nexoia.auth.user.model.UserRole;
import com.nexoia.auth.user.model.UserStatus;
import com.nexoia.auth.user.repository.UserAccountRepository;
import com.nexoia.knowledge.vault.service.VaultService;
import com.nexoia.permission.exception.PermissionDelegationDeniedException;
import com.nexoia.permission.model.ProfileKey;
import com.nexoia.permission.service.PermissionAdminService;
import com.nexoia.team.dto.AddTeamMemberRequest;
import com.nexoia.team.dto.CreateTeamRequest;
import com.nexoia.team.dto.CreateTeamVaultRequest;
import com.nexoia.team.dto.TeamMemberResponse;
import com.nexoia.team.dto.TeamResponse;
import com.nexoia.team.exception.TeamAdministrationDeniedException;
import com.nexoia.team.model.Team;
import com.nexoia.team.model.TeamMembership;
import com.nexoia.team.model.TeamRole;
import com.nexoia.team.repository.TeamMembershipRepository;
import com.nexoia.team.repository.TeamRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock TeamRepository teams;
    @Mock TeamMembershipRepository memberships;
    @Mock UserAccountRepository users;
    @Mock VaultService vaultService;
    @Mock AuditService audit;

    private TeamService service;
    private final UUID ownerId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();

    @BeforeEach void setUp() {
        service = new TeamService(teams, memberships, users, new PermissionAdminService(),
                vaultService, audit);
    }

    @Test void ownerCreatesATeamAndBecomesItsAdmin() {
        when(teams.saveAndFlush(any(Team.class))).thenAnswer(invocation -> persistedTeam(
                invocation.getArgument(0)));
        when(memberships.saveAndFlush(any(TeamMembership.class)))
                .thenAnswer(invocation -> persistedMembership(invocation.getArgument(0)));
        when(users.findById(ownerId)).thenReturn(Optional.of(user(ownerId, UserRole.OWNER, ProfileKey.OPERATOR)));

        TeamResponse response = service.createTeam(principal(ownerId, UserRole.OWNER),
                new CreateTeamRequest("Platform", ProfileKey.RESEARCHER, 100_000L));

        assertThat(response.name()).isEqualTo("Platform");
        assertThat(response.createdBy()).isEqualTo(ownerId);
        assertThat(response.defaultProfile()).isEqualTo(ProfileKey.RESEARCHER);
        assertThat(response.teamRole()).isEqualTo(TeamRole.ADMIN);
        assertThat(response.assignedProfile()).isEqualTo(ProfileKey.OPERATOR);
        assertThat(response.manageable()).isTrue();
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test void ownerAddsAMemberWithTheTeamDefaultProfile() {
        when(memberships.saveAndFlush(any(TeamMembership.class)))
                .thenAnswer(invocation -> persistedMembership(invocation.getArgument(0)));
        when(teams.findById(teamId)).thenReturn(Optional.of(team(ProfileKey.RESEARCHER)));
        when(users.findById(ownerId)).thenReturn(Optional.of(user(ownerId, UserRole.OWNER, ProfileKey.OPERATOR)));
        when(users.findById(targetId)).thenReturn(Optional.of(user(targetId, UserRole.MEMBER, ProfileKey.RESEARCHER)));
        when(memberships.existsByTeamIdAndUserId(teamId, targetId)).thenReturn(false);

        TeamMemberResponse response = service.addMember(principal(ownerId, UserRole.OWNER), teamId,
                new AddTeamMemberRequest(targetId, null, null));

        assertThat(response.teamRole()).isEqualTo(TeamRole.MEMBER);
        assertThat(response.name()).isEqualTo("U");
        assertThat(response.email()).isEqualTo("u@nexo.local");
        assertThat(response.assignedProfile()).isEqualTo(ProfileKey.RESEARCHER);
        assertThat(response.joinedAt()).isNotNull();
    }

    @Test void deniesAddingAMemberToATeamTheCallerDoesNotAdminister() {
        when(teams.findById(teamId)).thenReturn(Optional.of(team(ProfileKey.RESEARCHER)));
        when(memberships.existsByTeamIdAndUserIdAndTeamRole(teamId, adminId, TeamRole.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> service.addMember(principal(adminId, UserRole.ADMIN), teamId,
                new AddTeamMemberRequest(targetId, null, null)))
                .isInstanceOf(TeamAdministrationDeniedException.class);
    }

    @Test void deniesAssigningAProfileAboveTheTeamAdminsOwnCeiling() {
        when(teams.findById(teamId)).thenReturn(Optional.of(team(ProfileKey.RESEARCHER)));
        when(memberships.existsByTeamIdAndUserIdAndTeamRole(teamId, adminId, TeamRole.ADMIN)).thenReturn(true);
        when(users.findById(adminId)).thenReturn(Optional.of(user(adminId, UserRole.ADMIN, ProfileKey.RESEARCHER)));
        when(users.findById(targetId)).thenReturn(Optional.of(user(targetId, UserRole.MEMBER, ProfileKey.RESEARCHER)));
        when(memberships.existsByTeamIdAndUserId(teamId, targetId)).thenReturn(false);

        assertThatThrownBy(() -> service.addMember(principal(adminId, UserRole.ADMIN), teamId,
                new AddTeamMemberRequest(targetId, null, ProfileKey.OPERATOR)))
                .isInstanceOf(PermissionDelegationDeniedException.class);
    }

    @Test void teamAdminCreatesASharedVaultOwnedByTheTeam() {
        when(teams.findById(teamId)).thenReturn(Optional.of(team(ProfileKey.RESEARCHER)));
        when(memberships.existsByTeamIdAndUserIdAndTeamRole(teamId, adminId, TeamRole.ADMIN)).thenReturn(true);

        service.createVault(principal(adminId, UserRole.ADMIN), teamId,
                new CreateTeamVaultRequest("Shared KB", "team knowledge"));

        verify(vaultService)
                .createForTeam(adminId, teamId, "Shared KB", "team knowledge");
    }

    private Team persistedTeam(Team team) {
        Instant now = Instant.parse("2026-08-25T00:00:00Z");
        return Team.builder()
                .id(team.getId())
                .name(team.getName())
                .createdBy(team.getCreatedBy())
                .tokenBudgetLimit(team.getTokenBudgetLimit())
                .defaultProfile(team.getDefaultProfile())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private TeamMembership persistedMembership(TeamMembership membership) {
        return TeamMembership.builder()
                .id(membership.getId())
                .teamId(membership.getTeamId())
                .userId(membership.getUserId())
                .teamRole(membership.getTeamRole())
                .assignedProfile(membership.getAssignedProfile())
                .joinedAt(Instant.parse("2026-08-25T00:00:00Z"))
                .build();
    }

    private Team team(ProfileKey defaultProfile) {
        return Team.builder().id(teamId).name("Platform").createdBy(ownerId)
                .defaultProfile(defaultProfile).build();
    }

    private UserAccount user(UUID id, UserRole role, ProfileKey profile) {
        return UserAccount.builder().id(id).username("u").email("u@nexo.local").name("U")
                .role(role).status(UserStatus.ACTIVE).assignedProfile(profile).build();
    }

    private NexoUserPrincipal principal(UUID id, UserRole role) {
        return new NexoUserPrincipal(id, "u", "u@nexo.local", "U", Instant.now(), role, "hash", true);
    }
}
