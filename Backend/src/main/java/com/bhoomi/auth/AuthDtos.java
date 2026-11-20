package com.bhoomi.auth;

import com.bhoomi.user.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @Size(max = 32) String phone,
            @NotBlank @Size(min = 2, max = 160) String fullName,
            @NotBlank @Size(min = 12, max = 72) String password,
            @NotNull RegistrationType accountType,
            @Size(max = 160) String businessName) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 72) String password) {
    }

    public record ForgotPasswordRequest(
            @NotBlank @Email @Size(max = 320) String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Size(min = 43, max = 43) String token,
            @NotBlank @Size(min = 12, max = 72)
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,72}$",
                    message = "must contain lowercase, uppercase, numeric, and special characters")
            String newPassword) {
    }

    public record PasswordResetMessage(String message) {
    }

    public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
    }

    public record UserResponse(java.util.UUID id, String email, String fullName, UserRole role,
            String brokerApprovalStatus) {
    }
}
