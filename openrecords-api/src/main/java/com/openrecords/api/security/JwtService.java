package com.openrecords.api.security;

import com.openrecords.api.config.JwtProperties;
import com.openrecords.api.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Generates and validates JWT tokens.
 *
 * Two token types:
 * - Access tokens: short-lived (15 min), include user identity claims
 * - Refresh tokens: long-lived (30 days), include only user ID + token type
 *
 * Tokens are signed with HMAC-SHA256 using a server-side secret.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties props;
    private final SecretKey signingKey;

    public JwtService(JwtProperties props) {
        this.props = props;
        byte[] secretBytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                "JWT secret must be at least 32 bytes (256 bits) for HS256. Got " + secretBytes.length
            );
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        log.info("JwtService initialized with {} ms access token, {} ms refresh token",
            props.getAccessTokenMs(), props.getRefreshTokenMs());
    }

    /**
     * Generate a short-lived access token containing user identity claims.
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(props.getAccessTokenMs());

        return Jwts.builder()
            .issuer(props.getIssuer())
            .subject(String.valueOf(user.getId()))
            .claim(CLAIM_EMAIL, user.getEmail())
            .claim(CLAIM_ROLE, user.getRole().name())
            .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .id(UUID.randomUUID().toString())
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact();
    }

    /**
     * Generate a long-lived refresh token. Includes only user ID;
     * the actual session is validated against the refresh_tokens table.
     */
    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(props.getRefreshTokenMs());

        return Jwts.builder()
            .issuer(props.getIssuer())
            .subject(String.valueOf(user.getId()))
            .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .id(UUID.randomUUID().toString())
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact();
    }

    /**
     * Parse and validate a token, returning its claims.
     *
     * Throws JwtException (and subclasses) if the token is invalid:
     * - ExpiredJwtException — token expired
     * - SignatureException — signature doesn't match
     * - MalformedJwtException — token isn't a valid JWT
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .requireIssuer(props.getIssuer())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Extract the user ID from a valid token.
     */
    public Long extractUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    /**
     * Verify the token is an access token (not a refresh token).
     */
    public boolean isAccessToken(Claims claims) {
        return TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    /**
     * Verify the token is a refresh token (not an access token).
     */
    public boolean isRefreshToken(Claims claims) {
        return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }
}