package com.nexoia.team.model;

import com.nexoia.permission.model.ProfileKey;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * One user's membership in one Team: their Team-scoped role and the capability profile assigned to them
 * inside that Team, bounded by the authority of whoever added them.
 */
@Getter
@Builder
@Entity
@Table(name = "team_membership")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamMembership {

    @Id
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_role", nullable = false, length = 24)
    private TeamRole teamRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_profile", nullable = false, length = 24)
    private ProfileKey assignedProfile;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    public void assignProfile(ProfileKey assignedProfile) {
        this.assignedProfile = assignedProfile;
    }
}
