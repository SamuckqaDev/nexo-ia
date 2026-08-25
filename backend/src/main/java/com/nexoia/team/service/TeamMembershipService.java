package com.nexoia.team.service;

import com.nexoia.team.repository.TeamMembershipRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single, deterministic resolver of a user's authorization scope across owned and shared resources:
 * the ids a resource's {@code owner_id} may hold for that user to access it — the user themselves plus
 * every Team they belong to. Every read path that authorizes Team-owned resources resolves the scope
 * here, so the isolation boundary lives in one place.
 */
@Service
@RequiredArgsConstructor
public class TeamMembershipService {

    private final TeamMembershipRepository memberships;

    /** {@code [userId] + the ids of every Team the user is a member of.} Never empty. */
    @Transactional(readOnly = true)
    public List<UUID> accessibleOwnerIds(UUID userId) {
        List<UUID> ownerIds = new ArrayList<>();
        ownerIds.add(userId);
        memberships.findAllByUserIdOrderByJoinedAtAsc(userId)
                .forEach(membership -> ownerIds.add(membership.getTeamId()));
        return ownerIds;
    }
}
