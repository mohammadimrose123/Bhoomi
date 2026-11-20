package com.bhoomi.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import com.bhoomi.config.CorsConfiguration;
import com.bhoomi.config.SecurityConfiguration;
import com.bhoomi.user.UserRepository;

@WebMvcTest(value = AuthController.class,
        properties = "bhoomi.security.jwt-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@Import({SecurityConfiguration.class, CorsConfiguration.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private PasswordResetRateLimiter rateLimiter;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void knownAndUnknownEmailsReceiveTheSameGenericRecoveryResponse() throws Exception {
        when(rateLimiter.tryAcquire(any())).thenReturn(true);
        when(passwordResetService.requestPasswordReset(any())).thenReturn(true);

        String knownResponse = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"buyer@example.test\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value(
                        "If an account exists and email delivery succeeds, password reset instructions will be sent."))
                .andReturn().getResponse().getContentAsString();
        String unknownResponse = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown@example.test\"}"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(unknownResponse).isEqualTo(knownResponse);
    }

    @Test
    void passwordRecoveryRequestsAreRateLimited() throws Exception {
        when(rateLimiter.tryAcquire(any())).thenReturn(false);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"buyer@example.test\"}"))
                .andExpect(status().isTooManyRequests());
        verify(passwordResetService, never()).requestPasswordReset(any());
    }

    @Test
    void reportsRecoveryUnavailableWhenNoEmailProviderIsConfigured() throws Exception {
        when(rateLimiter.tryAcquire(any())).thenReturn(true);
        when(passwordResetService.requestPasswordReset(any())).thenReturn(false);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"buyer@example.test\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Password recovery is temporarily unavailable."));
    }

    @Test
    void invalidRegistrationReturnsFieldErrorsWithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "fullName": "A",
                                  "password": "short",
                                  "accountType": "BUYER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
        verify(authService, never()).register(any());
    }

    @Test
    void brokerCannotAccessBuyerEndpoints() throws Exception {
        mockMvc.perform(get("/api/buyer/favorites").with(SecurityMockMvcRequestPostProcessors.user("broker").roles("BROKER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void buyerCannotAccessAdminOrBrokerManagementEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/reviews").with(SecurityMockMvcRequestPostProcessors.user("buyer").roles("BUYER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/broker/site-visits").with(SecurityMockMvcRequestPostProcessors.user("buyer").roles("BUYER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedSiteVisitRequestIsRejected() throws Exception {
        mockMvc.perform(post("/api/listings/00000000-0000-0000-0000-000000000001/site-visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestedAt\":\"2030-01-01T10:00:00Z\",\"notes\":\"Visit\"}"))
                .andExpect(status().isUnauthorized());
    }
}
