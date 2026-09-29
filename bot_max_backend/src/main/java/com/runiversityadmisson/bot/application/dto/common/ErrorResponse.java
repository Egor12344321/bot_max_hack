package com.runiversityadmisson.bot.application.dto.common;


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
