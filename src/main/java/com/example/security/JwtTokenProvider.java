package com.example.security;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.entities.ERole;
import com.example.entities.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Emite y valida los tokens JWT (firmados con HMAC-SHA256).
 *
 * El {@code subject} del token es el email del usuario, de modo que el email
 * es la credencial de acceso. El rol viaja en el claim {@value #CLAIM_ROLE}.
 */
@Component
public class JwtTokenProvider {

    public static final String CLAIM_ROLE = "role";

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey secretKey;
    private final long expirationMillis;
    private final String issuer;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes:60}") long expirationMinutes,
            @Value("${app.jwt.issuer:rest-api-many-to-many-example}") String issuer) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMillis = TimeUnit.MINUTES.toMillis(expirationMinutes);
        this.issuer = issuer;
    }

    /** Genera un token JWT para el usuario indicado. */
    public String generateToken(User user) {
        Date now = new Date();

        return Jwts.builder()
                .subject(user.getEmail())
                .issuer(issuer)
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(secretKey)
                .compact();
    }

    /** Extrae el email (subject) del token. */
    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /** Extrae el rol (claim {@value #CLAIM_ROLE}) del token. */
    public ERole getRoleFromToken(String token) {
        String role = parseClaims(token).get(CLAIM_ROLE, String.class);
        return role == null ? null : ERole.valueOf(role);
    }

    /** @return true si el token tiene firma valida y no ha expirado. */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            logger.warn("Token JWT invalido: {}", ex.getMessage());
            return false;
        }
    }

    public long getExpirationMillis() {
        return expirationMillis;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}