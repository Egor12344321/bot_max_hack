package com.runiversityadmisson.bot.web.exception;


/**
 * Выбрасывается, когда initData от MAX WebApp невалиден:
 * подпись не сходится, отсутствует hash, отсутствует user_id и т.д.
 */
public class InvalidInitDataException extends RuntimeException {

    public InvalidInitDataException(String message) {
        super(message);
    }

    public InvalidInitDataException(String message, Throwable cause) {
        super(message, cause);
    }
}