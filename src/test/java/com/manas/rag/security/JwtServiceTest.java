package com.manas.rag.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.manas.rag.config.AppProperties;

class JwtServiceTest {

    private static JwtService service(String secret, long expiryMinutes) {
        return new JwtService(new AppProperties(new AppProperties.Jwt(secret, expiryMinutes), null));
    }

    @Test
    void tokenRoundTripsTheUserId() {
        JwtService jwt = service("test-secret-test-secret-test-secret-1234", 60);
        UUID userId = UUID.randomUUID();

        assertThat(jwt.parseUserId(jwt.createToken(userId))).contains(userId);
    }

    @Test
    void rejectsGarbageExpiredAndForeignTokens() {
        JwtService jwt = service("test-secret-test-secret-test-secret-1234", 60);
        UUID userId = UUID.randomUUID();

        assertThat(jwt.parseUserId("not-a-token")).isEmpty();
        assertThat(jwt.parseUserId(service("test-secret-test-secret-test-secret-1234", -1).createToken(userId)))
                .isEmpty();
        assertThat(jwt.parseUserId(service("another-secret-another-secret-another-1", 60).createToken(userId)))
                .isEmpty();
    }

    @Test
    void blankSecretFallsBackToARandomKey() {
        JwtService jwt = service("", 60);
        UUID userId = UUID.randomUUID();

        assertThat(jwt.parseUserId(jwt.createToken(userId))).contains(userId);
    }

    @Test
    void shortSecretIsRefused() {
        assertThatThrownBy(() -> service("too-short", 60)).isInstanceOf(IllegalStateException.class);
    }
}
