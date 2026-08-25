package com.nexoia.permission.model;

/** How much of a sensitive-but-lawful content area a profile may produce. */
public enum ContentAllowance {
    /** Generate freely (lawful content). */
    FULL,
    /** Explain factually or clinically, but do not generate graphic material. */
    PARTIAL,
    /** Refuse, stating the area is not enabled for this profile. */
    BLOCK
}
