package com.nexoia.auth.user.model;

public enum UserRole {
    /** System owner (the "root" of the governance model): unbounded, created only at bootstrap. */
    OWNER,
    /** Delegated administrator: manages members and groups within their own authority ceiling. */
    ADMIN,
    /** Regular user: operates Nexo within the profile assigned to them. */
    MEMBER
}
