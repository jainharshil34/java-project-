package com.civicheat.security;

import com.civicheat.config.CivicHeatProperties;
import com.civicheat.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterTest {

    private CivicHeatProperties properties;
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        properties = new CivicHeatProperties();
        properties.getRateLimit().setEnabled(true);
        properties.getRateLimit().setMaxRequestsPerMinute(5);
        rateLimiter = new RateLimiter(properties);
    }

    @Test
    @DisplayName("Allows requests within configured rate limit")
    void checkLimit_underLimit_succeeds() {
        String client = "192.168.1.100";
        for (int i = 0; i < 5; i++) {
            assertThatCode(() -> rateLimiter.checkLimit(client)).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("Throws RateLimitExceededException when requests exceed threshold")
    void checkLimit_exceedsLimit_throwsException() {
        String client = "192.168.1.101";
        for (int i = 0; i < 5; i++) {
            rateLimiter.checkLimit(client);
        }

        assertThatThrownBy(() -> rateLimiter.checkLimit(client))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Rate limit exceeded");
    }

    @Test
    @DisplayName("Rate limiting bypassed when disabled in configuration")
    void checkLimit_disabled_bypassesCheck() {
        properties.getRateLimit().setEnabled(false);
        String client = "192.168.1.102";

        for (int i = 0; i < 20; i++) {
            assertThatCode(() -> rateLimiter.checkLimit(client)).doesNotThrowAnyException();
        }
    }
}
