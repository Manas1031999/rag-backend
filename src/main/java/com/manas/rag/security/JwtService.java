package com.manas.rag.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.manas.rag.config.AppProperties;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int MIN_KEY_BYTES = 32;

    private final SecretKey key;
    private final Duration expiry;

    public JwtService(AppProperties properties) {
        this.key = Keys.hmacShaKeyFor(keyBytes(properties.jwt().secret()));
        this.expiry = Duration.ofMinutes(properties.jwt().expiryMinutes());
    }

    private static byte[] keyBytes(String secret) {
        if (secret == null || secret.isBlank()) {
            log.warn("JWT_SECRET is not set: using a random key, so tokens stop working when the app restarts.");
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            return random;
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least " + MIN_KEY_BYTES + " characters long.");
        }
        return bytes;
    }

    public String createToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiry)))
                .signWith(key)
                .compact();
    }

    /** Returns the user id inside the token, or empty if the token is invalid or expired. */
    public Optional<UUID> parseUserId(String token) {
        try {
            String subject = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload().getSubject();
            return Optional.of(UUID.fromString(subject));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public long expirySeconds() {
        return expiry.toSeconds();
    }
}
