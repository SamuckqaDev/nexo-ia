package com.nexoia.conversation.inference.context;

import com.nexoia.permission.model.ContentMatrix;
import com.nexoia.permission.model.ContentAllowance;
import com.nexoia.permission.model.UnlockLevel;
import java.util.List;

/**
 * The resolved permission state rendered into the capability envelope: the active profile and level, the
 * independent per-area content matrix, and the human labels of the capabilities the model does not have at
 * this level. It lets the model report its boundary truthfully and name the honest unlock path.
 *
 * <p>Capability level and content policy are separate axes on purpose (see
 * {@code docs/PERMISSION_PROFILES.md}): the level never restricts a topic, and the content matrix never
 * attaches or removes a tool.
 */
public record PermissionCapability(
        String profileName,
        UnlockLevel level,
        ContentMatrix contentMatrix,
        List<String> lockedCapabilities) {

    public PermissionCapability {
        lockedCapabilities = lockedCapabilities == null ? List.of() : List.copyOf(lockedCapabilities);
    }

    public static PermissionCapability none() {
        return new PermissionCapability(
                "Locked", UnlockLevel.L0_OBSERVER,
                ContentMatrix.uniform(ContentAllowance.BLOCK), List.of());
    }
}
