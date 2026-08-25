package com.nexoia.permission.model;

/** The fixed risk class of a {@link CapabilityFamily}, independent of who requests it. */
public enum RiskClass {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
    /** Never exposed to the model under any level or profile. */
    PROHIBITED
}
