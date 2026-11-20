package com.bhoomi.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordResetRateLimiterTest {

    @Test
    void allowsFiveRequestsThenRejectsFurtherRequestsForTheClient() {
        PasswordResetRateLimiter limiter = new PasswordResetRateLimiter();

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThat(limiter.tryAcquire("192.0.2.10")).isTrue();
        }

        assertThat(limiter.tryAcquire("192.0.2.10")).isFalse();
        assertThat(limiter.tryAcquire("192.0.2.11")).isTrue();
    }
}
