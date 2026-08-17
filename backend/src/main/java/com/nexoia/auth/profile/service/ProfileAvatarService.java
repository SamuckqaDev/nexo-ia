package com.nexoia.auth.profile.service;

import com.nexoia.auth.profile.dto.ProfileAvatarContent;
import com.nexoia.auth.profile.exception.InvalidProfileAvatarException;
import com.nexoia.auth.profile.exception.ProfileAvatarNotFoundException;
import com.nexoia.auth.profile.model.ProfileAvatar;
import com.nexoia.auth.profile.repository.ProfileAvatarRepository;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProfileAvatarService {
    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final Map<String, byte[]> SIGNATURES = Map.of(
            "image/png", new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47},
            "image/jpeg", new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff},
            "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46});

    private final ProfileAvatarRepository repository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ProfileAvatarContent get(UUID userId) {
        ProfileAvatar avatar = repository.findById(userId)
                .orElseThrow(ProfileAvatarNotFoundException::new);
        return new ProfileAvatarContent(avatar.getContent(), avatar.getContentType());
    }

    @Transactional
    public void save(UUID userId, MultipartFile file) {
        try {
            String contentType = file.getContentType();
            byte[] content = file.getBytes();
            if (contentType == null || file.isEmpty() || content.length > MAX_BYTES
                    || !hasSignature(contentType, content)) {
                throw new InvalidProfileAvatarException();
            }
            repository.save(ProfileAvatar.builder().userId(userId).contentType(contentType)
                    .content(content).updatedAt(clock.instant()).build());
        } catch (IOException exception) {
            throw new InvalidProfileAvatarException();
        }
    }

    @Transactional
    public void delete(UUID userId) {
        repository.deleteById(userId);
    }

    private boolean hasSignature(String contentType, byte[] content) {
        byte[] signature = SIGNATURES.get(contentType);
        if (signature == null || content.length < signature.length) return false;
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) return false;
        }
        return true;
    }
}
