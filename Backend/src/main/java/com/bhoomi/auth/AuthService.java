package com.bhoomi.auth;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.bhoomi.broker.BrokerProfile;
import com.bhoomi.broker.BrokerProfileRepository;
import com.bhoomi.user.UserEntity;
import com.bhoomi.user.UserRepository;
import com.bhoomi.user.UserRole;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BrokerProfileRepository brokerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final java.time.Duration tokenLifetime;

    public AuthService(UserRepository userRepository, BrokerProfileRepository brokerProfileRepository,
            PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
            @Value("${bhoomi.security.jwt-issuer}") String issuer,
            @Value("${bhoomi.security.jwt-expiration}") java.time.Duration tokenLifetime) {
        this.userRepository = userRepository;
        this.brokerProfileRepository = brokerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.tokenLifetime = tokenLifetime;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        UserRole role = request.accountType() == RegistrationType.BROKER ? UserRole.BROKER : UserRole.BUYER;
        UserEntity user = userRepository.save(new UserEntity(normalizedEmail,
                normalizeOptional(request.phone()), passwordEncoder.encode(request.password()),
                request.fullName().trim(), role));
        String approvalStatus = null;
        if (role == UserRole.BROKER) {
            String businessName = request.businessName() == null ? "" : request.businessName().trim();
            if (businessName.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "businessName is required for broker registration");
            }
            BrokerProfile profile = brokerProfileRepository.save(new BrokerProfile(user.getId(), businessName));
            approvalStatus = profile.getApprovalStatus().name();
        }
        return issueToken(user, approvalStatus);
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(UserEntity::isEnabled)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Email or password is incorrect"));
        String approvalStatus = user.getRole() == UserRole.BROKER
                ? brokerProfileRepository.findByUserId(user.getId())
                        .map(profile -> profile.getApprovalStatus().name()).orElse(null)
                : null;
        return issueToken(user, approvalStatus);
    }

    private AuthDtos.AuthResponse issueToken(UserEntity user, String brokerApprovalStatus) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(tokenLifetime))
                .subject(user.getId().toString())
                .claim("roles", List.of(user.getRole().name()))
                .claim("credentialVersion", user.getCredentialVersion())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        AuthDtos.UserResponse userResponse = new AuthDtos.UserResponse(
                user.getId(), user.getEmail(), user.getFullName(), user.getRole(), brokerApprovalStatus);
        return new AuthDtos.AuthResponse(token, "Bearer", tokenLifetime.toSeconds(), userResponse);
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
