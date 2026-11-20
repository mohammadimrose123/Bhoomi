package com.bhoomi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.bhoomi.broker.BrokerProfile;
import com.bhoomi.broker.BrokerProfileRepository;
import com.bhoomi.user.UserEntity;
import com.bhoomi.user.UserRepository;
import com.bhoomi.user.UserRole;

import static org.mockito.Mockito.mock;

class AuthServiceTest {

    private UserRepository userRepository;
    private BrokerProfileRepository brokerRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private JwtEncoder jwtEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        brokerRepository = mock(BrokerProfileRepository.class);
        passwordEncoder = new BCryptPasswordEncoder(4);
        jwtEncoder = mock(JwtEncoder.class);
        authService = new AuthService(userRepository, brokerRepository, passwordEncoder, jwtEncoder,
                "bhoomi-test", Duration.ofHours(1));
        when(jwtEncoder.encode(any())).thenAnswer(invocation -> Jwt.withTokenValue("signed-test-token")
                .header("alg", "HS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .subject(UUID.randomUUID().toString())
                .build());
    }

    @Test
    void registrationHashesPasswordAndDefaultsToBuyer() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsByEmailIgnoreCase("buyer@example.test")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", userId);
            return user;
        });

        AuthDtos.AuthResponse response = authService.register(new AuthDtos.RegisterRequest(
                "Buyer@example.test", null, "Sample Buyer", "a-very-long-password",
                RegistrationType.BUYER, null));

        assertThat(response.user().role()).isEqualTo(UserRole.BUYER);
        assertThat(response.accessToken()).isEqualTo("signed-test-token");
        verify(userRepository).save(any(UserEntity.class));
        verify(brokerRepository, never()).save(any(BrokerProfile.class));
        org.mockito.ArgumentCaptor<UserEntity> captor = org.mockito.ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity storedUser = captor.getValue();
        assertThat(storedUser.getPasswordHash()).isNotEqualTo("a-very-long-password");
        assertThat(passwordEncoder.matches("a-very-long-password", storedUser.getPasswordHash())).isTrue();
        assertThat(storedUser.getRole()).isEqualTo(UserRole.BUYER);
        assertThat(storedUser.getEmail()).isEqualTo("buyer@example.test");
    }

    @Test
    void brokerRegistrationCreatesPendingProfileWithoutTrustingRoleField() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsByEmailIgnoreCase("broker@example.test")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", userId);
            return user;
        });
        when(brokerRepository.save(any(BrokerProfile.class))).thenAnswer(invocation -> {
            BrokerProfile profile = invocation.getArgument(0);
            ReflectionTestUtils.setField(profile, "id", UUID.randomUUID());
            return profile;
        });

        AuthDtos.AuthResponse response = authService.register(new AuthDtos.RegisterRequest(
                "broker@example.test", null, "Sample Broker", "another-strong-password",
                RegistrationType.BROKER, "Local Realty"));

        assertThat(response.user().role()).isEqualTo(UserRole.BROKER);
        assertThat(response.user().brokerApprovalStatus()).isEqualTo("PENDING");
    }
}
