package com.nexoia.permission.model;

/** The resolved decision for one capability family in one request. */
public enum CapabilityDecision {

    /** Attached and executable within its caps. */
    ALLOWED,

    /** Attached as a gated callback that runs only after explicit, fresh, per-action consent. */
    REQUIRES_APPROVAL,

    /** Not attached; the model is told the family is locked, with the honest unlock path. */
    DENIED
}
