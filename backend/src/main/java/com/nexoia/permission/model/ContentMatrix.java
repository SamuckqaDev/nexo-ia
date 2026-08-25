package com.nexoia.permission.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * The per-area content allowance carried by a {@link PermissionProfile}. An area absent from the map
 * defaults to {@link ContentAllowance#BLOCK} — fail-closed: an undeclared area is not generated.
 *
 * <p>This is the content axis, independent of capability: it never adds or removes a tool, and a
 * capability level never restricts a topic. See {@code docs/ORGANIZATIONS_AND_GOVERNANCE.md} section 6.
 */
public record ContentMatrix(Map<ContentArea, ContentAllowance> allowances) {

    public ContentMatrix {
        allowances = allowances == null ? Map.of() : Map.copyOf(allowances);
    }

    public ContentAllowance allowance(ContentArea area) {
        return allowances.getOrDefault(area, ContentAllowance.BLOCK);
    }

    /** Every area at the same allowance — used for the fully-open root profile. */
    public static ContentMatrix uniform(ContentAllowance allowance) {
        EnumMap<ContentArea, ContentAllowance> map = new EnumMap<>(ContentArea.class);
        for (ContentArea area : ContentArea.values()) {
            map.put(area, allowance);
        }
        return new ContentMatrix(map);
    }
}
