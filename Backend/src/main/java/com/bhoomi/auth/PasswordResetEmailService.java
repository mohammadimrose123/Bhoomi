package com.bhoomi.auth;

public interface PasswordResetEmailService {

    boolean isConfigured();

    void sendResetLink(String emailAddress, String resetUrl);
}
