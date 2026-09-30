package com.runiversityadmisson.bot.presentation.exception;

public class OnboardingRequiredException extends RuntimeException {
	public OnboardingRequiredException() {
		super("Сначала завершите ввод данных в MAX-боте, затем откройте мини-приложение повторно");
	}
}
