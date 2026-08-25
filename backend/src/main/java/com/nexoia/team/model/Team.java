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
import org.hibernate.annotations.UpdateTimestamp;

/**
 * The organization/tenant boundary of the governance model. A Team owns its members, shared knowledge,
 * token budget, media and artifacts, isolated from every other Team. See
 * {@code docs/ORGANIZATIONS_AND_GOVERNANCE.md}.
 */
@Getter
@Builder
@Entity
@Table(name = "team")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Team {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    /** Optional token quota all members' usage draws from; {@code null} means no cap (enforced later). */
    @Column(name = "token_budget_limit")
    private Long tokenBudgetLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_profile", nullable = false, length = 24)
    private ProfileKey defaultProfile;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void rename(String name) {
        this.name = name;
    }

    public void setTokenBudgetLimit(Long tokenBudgetLimit) {
        this.tokenBudgetLimit = tokenBudgetLimit;
    }
}
