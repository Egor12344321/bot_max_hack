package com.runiversityadmisson.bot.web.dto;

public record ExchangeAuthResponse(String accessToken, long expiresIn, SessionResponse session) {
}
