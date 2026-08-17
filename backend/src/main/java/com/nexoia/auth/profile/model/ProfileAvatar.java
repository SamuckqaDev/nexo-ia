package com.nexoia.auth.profile.model;

import jakarta.persistence.Column;
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

@Getter
@Builder
@Entity
@Table(name = "profile_avatar")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProfileAvatar {
    @Id private UUID userId;
    @Column(name = "content_type", nullable = false, length = 32) private String contentType;
    @Column(nullable = false, columnDefinition = "bytea") private byte[] content;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
