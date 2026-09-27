package com.runiversityadmisson.bot.bot.model;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateJsonTest {

	private final JsonMapper objectMapper = JsonMapper.builder().build();

	@Test
	void deserializesMaxBotStartedUpdateWithSnakeCaseFields() throws Exception {
		String json = """
				{
				  "update_type": "bot_started",
				  "timestamp": 1720000000,
				  "user": {
				    "user_id": 12345,
				    "first_name": "Иван",
				    "last_name": "Иванов"
				  }
				}
				""";

		Update update = objectMapper.readValue(json, Update.class);

		assertThat(update.getUpdateType()).isEqualTo("bot_started");
		assertThat(update.getTimestamp()).isEqualTo(1720000000L);
		assertThat(update.getUser().getUserId()).isEqualTo(12345L);
		assertThat(update.getUser().getFirstName()).isEqualTo("Иван");
		assertThat(update.getUser().getLastName()).isEqualTo("Иванов");
	}

	@Test
	void deserializesCallbackIdAndPayload() throws Exception {
		String json = """
				{
				  "update_type": "message_callback",
				  "callback": {
				    "callback_id": "callback-1",
				    "payload": "lang_kk",
				    "timestamp": 1720000000000,
				    "user": { "user_id": 54321, "first_name": "Тест", "is_bot": false }
				  }
				}
				""";

		Update update = objectMapper.readValue(json, Update.class);

		assertThat(update.getUpdateType()).isEqualTo("message_callback");
		assertThat(update.getCallback().getCallbackId()).isEqualTo("callback-1");
		assertThat(update.getCallback().getPayload()).isEqualTo("lang_kk");
		assertThat(update.getCallback().getUser().getUserId()).isEqualTo(54321L);
	}
}
