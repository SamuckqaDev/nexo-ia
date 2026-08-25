package com.nexoia.permission.model;

/**
 * A sensitive-but-lawful content domain whose allowance varies per profile. This is the tunable axis;
 * the fixed legal floor (genuinely illegal or serious-harm content) is intentionally NOT an area and is
 * never represented here. See {@code docs/ORGANIZATIONS_AND_GOVERNANCE.md} section 6.
 */
public enum ContentArea {
    SEXUAL_EXPLICIT,
    GRAPHIC_VIOLENCE,
    STRONG_LANGUAGE,
    MATURE_THEMES,
    MEDICAL_EXPLICIT
}
