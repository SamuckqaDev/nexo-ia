package com.nexoia.auth.user.model;

import com.nexoia.permission.model.ProfileKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Builder
@Entity
@Table(name = "user_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserAccount {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "display_name", nullable = false, length = 120)
    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_profile", nullable = false, length = 24)
    private ProfileKey assignedProfile;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void assignProfile(ProfileKey assignedProfile) {
        this.assignedProfile = assignedProfile;
    }

    public void changeStatus(UserStatus status) {
        this.status = status;
    }

    public void updateProfile(String username, String email, String name, LocalDate birthDate) {
        this.username = username;
        this.email = email;
        this.name = name;
        this.birthDate = birthDate;
    }
}
