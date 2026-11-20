package com.bhoomi.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final PasswordResetRateLimiter rateLimiter;

    public AuthController(AuthService authService, PasswordResetService passwordResetService,
            PasswordResetRateLimiter rateLimiter) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping(value = "/forgot-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthDtos.PasswordResetMessage> forgotPassword(
            @Valid @RequestBody AuthDtos.ForgotPasswordRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        enforceRateLimit(httpRequest);
        boolean available = passwordResetService.requestPasswordReset(request.email());
        AuthDtos.PasswordResetMessage message = new AuthDtos.PasswordResetMessage(available
                ? "If an account exists and email delivery succeeds, password reset instructions will be sent."
                : "Password recovery is temporarily unavailable.");
        return ResponseEntity.status(available ? HttpStatus.ACCEPTED : HttpStatus.SERVICE_UNAVAILABLE)
                .body(message);
    }

    @PostMapping(value = "/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        enforceRateLimit(httpRequest);
        passwordResetService.resetPassword(request.token(), request.newPassword());
    }

    private void enforceRateLimit(jakarta.servlet.http.HttpServletRequest request) {
        if (!rateLimiter.tryAcquire(request.getRemoteAddr())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many password recovery requests. Please try again later.");
        }
    }
}
