package com.nexoia.permission.model;

/**
 * The stable identifier of a built-in permission profile, persisted as the user's assigned profile.
 * Root-authored custom profiles are a later phase; today the assignable set is these built-ins.
 */
public enum ProfileKey {
    LOCKED,
    READER,
    RESEARCHER,
    BUILDER,
    OPERATOR
}
