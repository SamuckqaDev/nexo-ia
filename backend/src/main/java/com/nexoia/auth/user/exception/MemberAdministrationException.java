package com.nexoia.auth.user.exception;

import com.nexoia.shared.exception.ConflictApplicationException;

public class MemberAdministrationException extends ConflictApplicationException {
    public MemberAdministrationException() {
        super("Administrative session controls apply only to Member accounts");
    }
}
