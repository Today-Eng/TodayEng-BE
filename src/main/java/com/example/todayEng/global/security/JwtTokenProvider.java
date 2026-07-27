package com.example.todayEng.global.security;

import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    private final SecretKey key;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.access-token-expiration}") long accessExpiration,
                            @Value("${jwt.refresh-token-expiration}") long refreshExpiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public IssuedToken issueAccessToken(Long userId) { return issue(userId, "access", accessExpiration); }
    public IssuedToken issueRefreshToken(Long userId) { return issue(userId, "refresh", refreshExpiration); }

    private IssuedToken issue(Long userId, String type, long expirationMillis) {
        Instant now = Instant.now();
        Instant expires = now.plusMillis(expirationMillis);
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder().subject(userId.toString()).id(jti).claim("type", type)
                .issuedAt(Date.from(now)).expiration(Date.from(expires)).signWith(key).compact();
        return new IssuedToken(token, jti, LocalDateTime.ofInstant(expires, ZoneId.systemDefault()));
    }

    public Claims parse(String token, String requiredType) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            if (!requiredType.equals(claims.get("type", String.class))) throw new BaseException(ErrorCode.INVALID_TOKEN);
            return claims;
        } catch (ExpiredJwtException e) {
            throw new BaseException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BaseException(ErrorCode.INVALID_TOKEN);
        }
    }

    public record IssuedToken(String value, String jti, LocalDateTime expiresAt) {}
}
