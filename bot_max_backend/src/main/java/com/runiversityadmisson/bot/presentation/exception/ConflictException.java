package com.runiversityadmisson.bot.presentation.exception;

/** Данные изменились с момента, как клиент их загрузил (например, версия плана) → 409. */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
