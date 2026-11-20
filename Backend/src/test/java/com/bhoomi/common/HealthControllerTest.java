package com.bhoomi.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.bhoomi.config.CorsConfiguration;
import com.bhoomi.config.SecurityConfiguration;
import com.bhoomi.user.UserEntity;
import com.bhoomi.user.UserRepository;
import com.bhoomi.user.UserRole;

@WebMvcTest(value = HealthController.class,
        properties = "bhoomi.security.jwt-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@Import({SecurityConfiguration.class, CorsConfiguration.class})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void reportsHealthyStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"status":"UP"}
                        """));
    }

    @Test
    void allowsRequestsFromTheLocalReactDevelopmentServer() throws Exception {
        mockMvc.perform(options("/api/health")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void protectedApiReturnsSafeProblemResponseWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/reviews"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Authentication is required"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void buyerTokenCannotAccessAdminRoutes() throws Exception {
        Instant now = Instant.now();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("bhoomi").issuedAt(now).expiresAt(now.plusSeconds(60))
                        .subject(UUID.randomUUID().toString()).claim("roles", List.of("BUYER")).build()))
                .getTokenValue();

        mockMvc.perform(get("/api/admin/reviews").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void passwordChangeInvalidatesPreviouslyIssuedJwt() throws Exception {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity("buyer@example.test", null, "encoded-password",
                "Sample Buyer", UserRole.BUYER);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", userId);
        user.changePassword("new-encoded-password");
        org.mockito.Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Instant now = Instant.now();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("bhoomi").issuedAt(now).expiresAt(now.plusSeconds(60))
                        .subject(userId.toString()).claim("roles", List.of("BUYER"))
                        .claim("credentialVersion", 0).build()))
                .getTokenValue();

        mockMvc.perform(get("/api/admin/reviews").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
