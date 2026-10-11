package com.adherelens.identity.security;

import com.adherelens.identity.domain.Staff;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/** Issues tokens. Verifying them is done by JwtVerifier in auth-common. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expiryMinutes;

    public JwtService(@Value("${adherelens.jwt.secret}") String secret,
                      @Value("${adherelens.jwt.expiry-minutes}") long expiryMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMinutes = expiryMinutes;
    }

    public String issue(Staff staff) {
        Instant now = Instant.now();
        JwtBuilder builder = Jwts.builder()
                .subject(staff.getId().toString())
                .claim("role", staff.getRole().name())
                .claim("name", staff.getFullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiryMinutes, ChronoUnit.MINUTES)));
        if (staff.getHospitalId() != null) {
            builder.claim("hospitalId", staff.getHospitalId().toString());
        }
        return builder.signWith(key).compact();
    }
}
