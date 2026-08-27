package com.nexoia.workspace.change.service;

import com.nexoia.workspace.config.WorkspaceProperties;
import com.nexoia.workspace.config.WorkspaceStorage;
import com.nexoia.workspace.exception.WorkspaceUnavailableException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Stores private before/after text under the server artifact root, never under a public web path. */
@Component
public class WorkspaceChangeArtifactStore {

    private final WorkspaceProperties properties;
    private final WorkspaceStorage storage;

    public WorkspaceChangeArtifactStore(WorkspaceProperties properties, WorkspaceStorage storage) {
        this.properties = properties;
        this.storage = storage;
    }

    public StoredProposal write(
            UUID userId,
            UUID conversationId,
            UUID changeId,
            String beforeContent,
            String afterContent) {
        if (!storage.isArtifactRootAvailable()) {
            throw new WorkspaceUnavailableException();
        }
        Path directory = proposalDirectory(userId, conversationId, changeId);
        try {
            Files.createDirectories(directory);
            String beforeKey = beforeContent == null ? null : write(directory, "before.txt", beforeContent);
            String afterKey = afterContent == null ? null : write(directory, "after.txt", afterContent);
            return new StoredProposal(beforeKey, afterKey);
        } catch (IOException exception) {
            throw new WorkspaceUnavailableException();
        }
    }

    public String read(String key) {
        if (key == null) {
            return null;
        }
        try {
            return Files.readString(resolveKey(key), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new WorkspaceUnavailableException();
        }
    }

    private String write(Path directory, String name, String content) throws IOException {
        Path target = directory.resolve(name);
        Path temporary = Files.createTempFile(directory, ".nexo-change-", ".tmp");
        try {
            Files.writeString(
                    temporary,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING);
            moveAtomically(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
        return properties.artifactRootPath().relativize(target).toString().replace('\\', '/');
    }

    private Path proposalDirectory(UUID userId, UUID conversationId, UUID changeId) {
        return properties.artifactRootPath()
                .resolve(userId.toString())
                .resolve(conversationId.toString())
                .resolve("workspace-changes")
                .resolve(changeId.toString())
                .normalize();
    }

    private Path resolveKey(String key) {
        Path root = properties.artifactRootPath();
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) {
            throw new WorkspaceUnavailableException();
        }
        return resolved;
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public record StoredProposal(String beforeKey, String afterKey) {}
}
