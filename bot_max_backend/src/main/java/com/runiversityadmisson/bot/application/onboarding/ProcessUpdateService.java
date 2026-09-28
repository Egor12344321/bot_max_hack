package com.runiversityadmisson.bot.application.onboarding;

import com.runiversityadmisson.bot.infrastructure.external.max.dto.Update;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessUpdateService {

	private final OnboardingService onboardingService;

	public void process(Update update) {
		if (update == null || update.getUpdateType() == null) {
			log.debug("Получен пустой update");
			return;
		}

		log.debug("Update получен: type={}", update.getUpdateType());
		switch (update.getUpdateType()) {
			case "bot_started" -> handleBotStarted(update);
			case "message_created" -> handleMessage(update);
			case "message_callback" -> handleCallback(update);
			default -> log.debug("Тип update не обрабатывается: {}", update.getUpdateType());
		}
	}

	private void handleBotStarted(Update update) {
		Long userId = userIdOf(update.getUser());
		if (userId == null) {
			return;
		}
		log.info("Пользователь {} запустил бота", userId);
		onboardingService.start(userId);
	}

	private void handleMessage(Update update) {
		Update.Message message = update.getMessage();
		if (message == null) {
			log.debug("message_created без message");
			return;
		}
		Long userId = userIdOf(message.getSender());
		if (userId == null) {
			return;
		}
		String mid = message.getBody() == null ? null : message.getBody().getMid();
		String text = message.getBody() == null ? null : message.getBody().getText();
		log.debug("Сообщение от {} (mid={}, длина={})", userId, mid, text == null ? 0 : text.length());
		onboardingService.handleText(userId, text);
	}

	private void handleCallback(Update update) {
		Update.Callback callback = update.getCallback();
		if (callback == null) {
			log.debug("message_callback без callback");
			return;
		}
		Long userId = userIdOf(callback.getUser());
		if (userId == null) {
			return;
		}
		log.info("Callback от {}: payload={}", userId, callback.getPayload());
		onboardingService.handleCallback(userId, callback.getCallbackId(), callback.getPayload());
	}

	private Long userIdOf(Update.User user) {
		if (user == null || user.getUserId() == null) {
			log.debug("Update без user_id");
			return null;
		}
		return user.getUserId();
	}
}
