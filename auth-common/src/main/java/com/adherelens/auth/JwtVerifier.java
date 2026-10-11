package com.adherelens.auth;

import com.adherelens.common.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Verifies tokens issued by identity-service. Every service shares the same secret. */
public class JwtVerifier {

    private final SecretKey key;

    public JwtVerifier(String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Throws a JwtException if the token is invalid, tampered with, or expired. */
    public AuthUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        String hospital = claims.get("hospitalId", String.class);
        return new AuthUser(
                UUID.fromString(claims.getSubject()),
                hospital == null ? null : UUID.fromString(hospital),
                Role.valueOf(claims.get("role", String.class)),
                claims.get("name", String.class));
    }
}
