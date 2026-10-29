package com.splitpay;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {
    private final SecretKey key;
    private final long ttlMs;

    JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.ttl-minutes}") long ttlMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(UTF_8));
        this.ttlMs = ttlMinutes * 60_000;
    }

    public String issue(Long userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder().subject(userId.toString()).issuedAt(new Date(now))
                .expiration(new Date(now + ttlMs)).signWith(key).compact();
    }

    /** @return user id, or throws JwtException / IllegalArgumentException if invalid or expired */
    public Long parse(String token) {
        return Long.valueOf(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject());
    }
}
