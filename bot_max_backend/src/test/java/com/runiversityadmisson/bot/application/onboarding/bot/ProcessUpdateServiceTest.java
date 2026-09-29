package com.runiversityadmisson.bot.application.onboarding.bot;

import com.runiversityadmisson.bot.infrastructure.external.max.dto.Update;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.Mockito.*;

class ProcessUpdateServiceTest {
	@Test
	void callbackUsesClickingUserNotMessageSender() {
		Update update = JsonMapper.builder().build().readValue("""
				{"update_type":"message_callback","timestamp":1720000000000,
				 "callback":{"timestamp":1720000000000,"callback_id":"cb-1",
				 "payload":"lang_kk","user":{"user_id":42,"first_name":"Тест","is_bot":false}},
				 "message":{"sender":{"user_id":99,"first_name":"Бот","is_bot":true}}}
				""", Update.class);
		OnboardingService onboarding = mock(OnboardingService.class);
		new ProcessUpdateService(onboarding).process(update);
		verify(onboarding).handleCallback(42L, "cb-1", "lang_kk");
		verifyNoMoreInteractions(onboarding);
	}
}
