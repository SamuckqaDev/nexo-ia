package com.nexoia.team.model;

/** A member's authority within one Team. Distinct from the system-wide {@code UserRole}. */
public enum TeamRole {
    /** Manages this Team: adds and removes members and assigns their profiles, within their ceiling. */
    ADMIN,
    /** Uses the Team's shared resources within the profile assigned to them. */
    MEMBER
}
