package com.bhoomi.auth;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.bhoomi.common.AuditService;
import com.bhoomi.user.UserRepository;

@Service
public class PasswordResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetEmailService emailService;
    private final AuditService auditService;
    private final Duration tokenLifetime;
    private final String frontendBaseUrl;

    public PasswordResetService(UserRepository userRepository, PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder, PasswordResetEmailService emailService, AuditService auditService,
            @Value("${bhoomi.security.password-reset-expiration:PT15M}") Duration tokenLifetime,
            @Value("${bhoomi.frontend.base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.auditService = auditService;
        this.tokenLifetime = tokenLifetime;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public boolean requestPasswordReset(String email) {
        if (!emailService.isConfigured()) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        var user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(com.bhoomi.user.UserEntity::isEnabled);
        if (user.isEmpty()) {
            auditService.record(null, "PASSWORD_RESET_REQUESTED", "USER", null, Map.of());
            return true;
        }

        Instant now = Instant.now();
        var account = user.get();
        tokenRepository.invalidateUnusedTokens(account.getId(), now);
        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken);
        PasswordResetToken resetToken = tokenRepository.save(
                new PasswordResetToken(account.getId(), tokenHash, now.plus(tokenLifetime)));
        String resetUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .replacePath("/reset-password")
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();

        try {
            emailService.sendResetLink(account.getEmail(), resetUrl);
        } catch (org.springframework.mail.MailException exception) {
            resetToken.markUsed(now);
            auditService.record(account.getId(), "PASSWORD_RESET_DELIVERY_FAILED", "USER", account.getId(), Map.of());
            LOGGER.warn("Password reset email delivery failed");
            return true;
        }
        auditService.record(account.getId(), "PASSWORD_RESET_REQUESTED", "USER", account.getId(), Map.of());
        return true;
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (!isStrongPassword(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 12 characters and include lowercase, uppercase, numeric, and special characters");
        }

        Instant now = Instant.now();
        PasswordResetToken resetToken = tokenRepository.findUsableToken(hashToken(rawToken), now)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Reset link is invalid or expired"));
        var user = userRepository.findById(resetToken.getUserId())
                .filter(com.bhoomi.user.UserEntity::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Reset link is invalid or expired"));

        resetToken.markUsed(now);
        user.changePassword(passwordEncoder.encode(newPassword));
        auditService.record(user.getId(), "PASSWORD_CHANGED", "USER", user.getId(), Map.of());
    }

    private static boolean isStrongPassword(String password) {
        return password != null && password.length() >= 12 && password.length() <= 72
                && password.matches("(?s).*[a-z].*")
                && password.matches("(?s).*[A-Z].*")
                && password.matches("(?s).*\\d.*")
                && password.matches("(?s).*[^A-Za-z0-9].*");
    }

    private static String generateToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private static String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
