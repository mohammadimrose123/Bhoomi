package com.bhoomi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.util.ReflectionTestUtils;

import com.bhoomi.common.AuditService;
import com.bhoomi.broker.BrokerProfileRepository;
import com.bhoomi.user.UserEntity;
import com.bhoomi.user.UserRepository;
import com.bhoomi.user.UserRole;

class PasswordResetServiceTest {

    private UserRepository userRepository;
    private PasswordResetTokenRepository tokenRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private PasswordResetEmailService emailService;
    private AuditService auditService;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        tokenRepository = org.mockito.Mockito.mock(PasswordResetTokenRepository.class);
        passwordEncoder = new BCryptPasswordEncoder(4);
        emailService = org.mockito.Mockito.mock(PasswordResetEmailService.class);
        auditService = org.mockito.Mockito.mock(AuditService.class);
        service = new PasswordResetService(userRepository, tokenRepository, passwordEncoder, emailService,
                auditService, Duration.ofMinutes(15), "http://localhost:5173");
    }

    @Test
    void existingEmailCreatesHashedTokenAndSendsAResetLink() {
        UserEntity user = createUser();
        when(emailService.isConfigured()).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("buyer@example.test")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.ArgumentCaptor<String> linkCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<PasswordResetToken> tokenCaptor =
                org.mockito.ArgumentCaptor.forClass(PasswordResetToken.class);

        assertThat(service.requestPasswordReset(" Buyer@example.test ")).isTrue();

        verify(tokenRepository).invalidateUnusedTokens(
                org.mockito.ArgumentMatchers.eq(user.getId()), any(Instant.class));
        verify(tokenRepository).save(tokenCaptor.capture());
        verify(emailService).sendResetLink(org.mockito.ArgumentMatchers.eq(user.getEmail()), linkCaptor.capture());
        String link = linkCaptor.getValue();
        String rawToken = link.substring(link.indexOf("token=") + "token=".length());
        assertThat(rawToken).hasSize(43);
        assertThat(tokenCaptor.getValue().getTokenHash()).matches("[a-f0-9]{64}").isNotEqualTo(rawToken);
        assertThat(tokenCaptor.getValue().getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void unknownEmailUsesTheSameAcceptedServiceOutcome() {
        when(emailService.isConfigured()).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("unknown@example.test")).thenReturn(Optional.empty());

        assertThat(service.requestPasswordReset("unknown@example.test")).isTrue();

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendResetLink(anyString(), anyString());
    }

    @Test
    void unconfiguredEmailDeliveryDoesNotCreateTokensOrClaimSuccess() {
        when(emailService.isConfigured()).thenReturn(false);

        assertThat(service.requestPasswordReset("buyer@example.test")).isFalse();

        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void emailDeliveryFailureDoesNotLeaveAUsableResetTokenOrExposeTheFailure() {
        UserEntity user = createUser();
        when(emailService.isConfigured()).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("buyer@example.test")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("delivery unavailable"))
                .when(emailService).sendResetLink(anyString(), anyString());
        org.mockito.ArgumentCaptor<PasswordResetToken> tokenCaptor =
                org.mockito.ArgumentCaptor.forClass(PasswordResetToken.class);

        assertThat(service.requestPasswordReset("buyer@example.test")).isTrue();

        verify(tokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getUsedAt()).isNotNull();
        verify(auditService).record(user.getId(), "PASSWORD_RESET_DELIVERY_FAILED",
                "USER", user.getId(), java.util.Map.of());
    }

    @Test
    void validTokenChangesPasswordAndInvalidatesPreviouslyIssuedJwtVersion() {
        UserEntity user = createUser();
        PasswordResetToken resetToken = new PasswordResetToken(user.getId(), "hash",
                Instant.now().plus(Duration.ofMinutes(15)));
        when(tokenRepository.findUsableToken(anyString(), any(Instant.class))).thenReturn(Optional.of(resetToken));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        service.resetPassword("A".repeat(43), "New-Strong-Pass42!");

        assertThat(resetToken.getUsedAt()).isNotNull();
        assertThat(passwordEncoder.matches("Old-Strong-Pass42!", user.getPasswordHash())).isFalse();
        assertThat(passwordEncoder.matches("New-Strong-Pass42!", user.getPasswordHash())).isTrue();
        assertThat(user.getCredentialVersion()).isEqualTo(1);
        verify(auditService).record(user.getId(), "PASSWORD_CHANGED", "USER", user.getId(), java.util.Map.of());

        when(userRepository.findByEmailIgnoreCase("buyer@example.test")).thenReturn(Optional.of(user));
        JwtEncoder jwtEncoder = org.mockito.Mockito.mock(JwtEncoder.class);
        when(jwtEncoder.encode(any())).thenAnswer(invocation -> Jwt.withTokenValue("new-access-token")
                .header("alg", "HS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .subject(user.getId().toString())
                .build());
        AuthService authService = new AuthService(userRepository,
                org.mockito.Mockito.mock(BrokerProfileRepository.class), passwordEncoder, jwtEncoder,
                "bhoomi-test", Duration.ofHours(1));

        assertThatThrownBy(() -> authService.login(
                new AuthDtos.LoginRequest(user.getEmail(), "Old-Strong-Pass42!")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email or password is incorrect");
        assertThat(authService.login(new AuthDtos.LoginRequest(
                user.getEmail(), "New-Strong-Pass42!")).accessToken()).isEqualTo("new-access-token");
    }

    @Test
    void invalidExpiredOrReusedTokensAreRejected() {
        when(tokenRepository.findUsableToken(anyString(), any(Instant.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("A".repeat(43), "New-Strong-Pass42!"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Reset link is invalid or expired");
    }

    @Test
    void expiredTokensAreRejected() {
        when(tokenRepository.findUsableToken(anyString(), any(Instant.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("B".repeat(43), "New-Strong-Pass42!"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Reset link is invalid or expired");
    }

    @Test
    void reusedTokensAreRejected() {
        when(tokenRepository.findUsableToken(anyString(), any(Instant.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("C".repeat(43), "New-Strong-Pass42!"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Reset link is invalid or expired");
    }

    @Test
    void olderResetTokensAreInvalidatedWhenANewRequestIsMade() {
        UserEntity user = createUser();
        when(emailService.isConfigured()).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("buyer@example.test")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.requestPasswordReset("buyer@example.test");

        verify(tokenRepository).invalidateUnusedTokens(
                org.mockito.ArgumentMatchers.eq(user.getId()), any(Instant.class));
    }

    @Test
    void weakPasswordsAreRejectedBeforeLookingUpAToken() {
        assertThatThrownBy(() -> service.resetPassword("A".repeat(43), "weak-password"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("at least 12 characters");
        verify(tokenRepository, never()).findUsableToken(anyString(), any(Instant.class));
    }

    private UserEntity createUser() {
        UserEntity user = new UserEntity("buyer@example.test", null, passwordEncoder.encode("Old-Strong-Pass42!"),
                "Sample Buyer", UserRole.BUYER);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
