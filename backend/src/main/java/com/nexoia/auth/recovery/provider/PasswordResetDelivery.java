package com.nexoia.auth.recovery.provider;

public interface PasswordResetDelivery {

    void send(String email, String name, String token);
}
