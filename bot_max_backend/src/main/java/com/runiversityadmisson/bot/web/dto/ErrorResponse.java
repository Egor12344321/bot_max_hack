package com.runiversityadmisson.bot.web.dto;


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