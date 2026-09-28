package com.runiversityadmisson.bot.application.dto.response;


/**
 * Стандартный ответ с ошибкой.
 *
 * @param code    машиночитаемый код ошибки
 * @param message человекочитаемое описание
 */
public record ErrorResponse(
        String code,
        String message
) {}
