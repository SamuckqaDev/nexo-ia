package com.nexoia.auth.profile.repository;

import com.nexoia.auth.profile.model.ProfileAvatar;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileAvatarRepository extends JpaRepository<ProfileAvatar, UUID> {
}
