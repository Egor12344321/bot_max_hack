package com.runiversityadmisson.bot.web.auth;

public class InvalidInitDataException extends RuntimeException {

	public InvalidInitDataException() {
		super("Недействительные данные запуска MAX");
	}
}
