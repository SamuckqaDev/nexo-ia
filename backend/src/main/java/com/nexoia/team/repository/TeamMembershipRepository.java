package com.nexoia.team.repository;

import com.nexoia.team.model.TeamMembership;
import com.nexoia.team.model.TeamRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMembershipRepository extends JpaRepository<TeamMembership, UUID> {

    List<TeamMembership> findAllByUserIdOrderByJoinedAtAsc(UUID userId);

    List<TeamMembership> findAllByTeamIdOrderByJoinedAtAsc(UUID teamId);

    Optional<TeamMembership> findByTeamIdAndUserId(UUID teamId, UUID userId);

    boolean existsByTeamIdAndUserIdAndTeamRole(UUID teamId, UUID userId, TeamRole teamRole);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);
}
