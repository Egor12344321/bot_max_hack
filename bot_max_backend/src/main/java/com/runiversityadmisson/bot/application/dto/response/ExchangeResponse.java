package com.runiversityadmisson.bot.application.dto.response;


import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ответ на exchange: JWT + публичная сессия пользователя.
 *
 * @param accessToken JWT для последующих запросов
 * @param expiresIn   время жизни токена в секундах
 * @param session     публичная сессия пользователя
 */
public record ExchangeResponse(
        String accessToken,
        Integer expiresIn,
        SessionResponse session
) {

    public record SessionResponse(
            UUID id,
            String platform,
            String language,
            String countryCode,
            LocalDateTime createdAt
    ) {}
}
