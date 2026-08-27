package com.nexoia.workspace.repository;

import com.nexoia.workspace.model.WorkspaceBinding;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceBindingRepository extends JpaRepository<WorkspaceBinding, UUID> {

    List<WorkspaceBinding> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    Optional<WorkspaceBinding> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    Optional<WorkspaceBinding> findByDeviceIdAndLocalBindingId(UUID deviceId, String localBindingId);
}
