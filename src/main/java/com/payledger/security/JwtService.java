package com.payledger.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] secretBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("payledger.security.jwt.secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String createToken(PayLedgerUserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.getExpiryMinutes() * 60);
        var builder = Jwts.builder()
                .subject(principal.getUsername())
                .claim("uid", principal.getUserId().toString())
                .claim("role", principal.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry));
        if (principal.getMerchantId() != null) {
            builder.claim("merchantId", principal.getMerchantId().toString());
        }
        return builder.signWith(key).compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID merchantIdClaim(Claims claims) {
        Object value = claims.get("merchantId");
        return value == null ? null : UUID.fromString(value.toString());
    }
}
