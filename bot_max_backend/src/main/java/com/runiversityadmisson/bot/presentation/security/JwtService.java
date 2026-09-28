package com.runiversityadmisson.bot.presentation.security;


import com.runiversityadmisson.bot.presentation.exception.InvalidJwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Генерация и валидация JWT для мини-аппа.
 *
 * <p>Токен содержит:
 * <ul>
 *     <li>{@code sub} — UUID пользователя</li>
 *     <li>{@code iat} — время выпуска</li>
 *     <li>{@code exp} — время истечения (TTL из конфига)</li>
 * </ul>
 */
@Service
@Slf4j
public class JwtService {

    private final SecretKey secretKey;
    /**
     * -- GETTER --
     *  Возвращает TTL токена в секундах.
     */
    @Getter
    private final long ttlSeconds;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.ttl:3600}") long ttlSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * Генерирует JWT для пользователя.
     *
     * @param userId UUID пользователя
     * @return подписанный JWT
     */
    public String generate(UUID userId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(ttlSeconds);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Извлекает UUID пользователя из токена.
     *
     * @param token JWT
     * @return UUID пользователя
     * @throws InvalidJwtException если токен невалиден или истёк
     */
    public UUID extractUserId(String token) {
        try {
            Claims claims = parse(token);
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                throw new InvalidJwtException("JWT subject is missing");
            }
            return UUID.fromString(subject);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidJwtException("Invalid token: " + exception.getMessage(), exception);
        }
    }

    /**
     * Проверяет, валиден ли токен (подпись + срок жизни).
     *
     * @param token JWT
     * @return true, если токен валиден
     */
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            log.debug("JWT validation failed: {}", exception.getMessage());
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
