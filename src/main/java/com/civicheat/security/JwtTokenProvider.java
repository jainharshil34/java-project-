package com.civicheat.security;

import com.civicheat.config.CivicHeatProperties;
import com.civicheat.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Utility component for generating, parsing, and validating JSON Web Tokens (JWT).
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final CivicHeatProperties properties;
    private final SecretKey key;

    public JwtTokenProvider(CivicHeatProperties properties) {
        this.properties = properties;
        byte[] keyBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        // Ensure secret key is at least 256 bits (32 bytes)
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            this.key = Keys.hmacShaKeyFor(padded);
        } else {
            this.key = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    /**
     * Generates a signed JWT token for the authenticated user.
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        long expiryHours = properties.getJwt().getExpiryHours();
        Instant expiry = now.plus(expiryHours, ChronoUnit.HOURS);

        var builder = Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("fullName", user.getFullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key);

        if (user.getDepartment() != null) {
            builder.claim("departmentId", user.getDepartment().getId().toString());
            builder.claim("departmentName", user.getDepartment().getName());
        }

        return builder.compact();
    }

    /**
     * Extracts all claims from a JWT token.
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the user email (subject) from a JWT token.
     */
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extracts the user ID from a JWT token.
     */
    public UUID extractUserId(String token) {
        String userId = extractClaims(token).get("userId", String.class);
        return userId != null ? UUID.fromString(userId) : null;
    }

    /**
     * Validates whether the token signature and expiration are valid.
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Returns token expiration duration in seconds.
     */
    public long getExpirationInSeconds() {
        return properties.getJwt().getExpiryHours() * 3600;
    }
}
