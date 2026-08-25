package com.nexoia.permission.model;

/**
 * The content axis — what subjects Nexo may discuss or generate — kept deliberately separate from the
 * capability {@link UnlockLevel}. Raising or lowering a permission level never changes this, and this
 * never changes which tools are attached.
 *
 * <p>A narrow legal/safety floor (genuinely illegal or serious-harm content) is always enforced and is
 * intentionally <em>not</em> represented here: it is not a configurable stance.
 */
public enum ContentStance {

    /** Default. Answer lawful requests directly, including sensitive/adult/controversial topics. */
    STANDARD,

    /** Optional stricter stance for shared/work deployments: declines explicit generation, still explains. */
    RESTRICTED
}
