package com.runiversityadmisson.bot.application.dto.request;


import jakarta.validation.constraints.NotBlank;

/**
 * Запрос от мини-аппа на обмен initData на JWT.
 *
 * @param launchParams сырые подписанные параметры от MAX WebApp (window.WebApp.initData)
 */
public record ExchangeRequest(
        @NotBlank(message = "launchParams is required")
        String launchParams
) {}
