package com.runiversityadmisson.bot.infrastructure.external.max;

import com.runiversityadmisson.bot.infrastructure.external.max.dto.NewMessageBody;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MaxBotClientTest {
	@Test
	void registersRestartInCommandMenu() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://example.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://example.test/me/commands"))
				.andExpect(method(HttpMethod.PATCH))
				.andExpect(header("Authorization", "test-token"))
				.andExpect(content().json("""
						{"commands":[{"name":"restart","description":"Начать заново"}]}
						"""))
				.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
		new MaxBotClient("test-token", builder.build()).registerCommands();
		server.verify();
	}

	@Test
	void skipsRegistrationWithoutToken() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://example.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		new MaxBotClient("", builder.build()).registerCommands();
		server.verify();
	}

	@Test
	void callbackIdIsQueryParameterAndNotificationIsBody() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://example.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://example.test/answers?callback_id=callback-1"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "test-token"))
				.andExpect(content().json("{\"notification\":\"Принято\"}"))
				.andExpect(jsonPath("$.callback_id").doesNotExist())
				.andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));
		new MaxBotClient("test-token", builder.build()).answerCallback("callback-1", "Принято");
		server.verify();
	}

	@Test
	void miniAppMessageContainsRequiredWebAppAndOmitsNullNotify() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://example.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("https://example.test/messages?user_id=42"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.attachments[0].type").value("inline_keyboard"))
				.andExpect(jsonPath("$.attachments[0].payload.buttons[0][0].web_app").value("test_bot"))
				.andExpect(jsonPath("$.attachments[0].payload.buttons[0][0].type").value("open_app"))
				.andExpect(jsonPath("$.notify").doesNotExist())
				.andExpect(jsonPath("$.disableLinkPreview").doesNotExist())
				.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
		NewMessageBody body = NewMessageBody.builder().text("Готово")
				.attachment(NewMessageBody.Attachment.inlineKeyboard(List.of(List.of(
						NewMessageBody.Button.openApp("Открыть", "user_42", "test_bot")))))
				.build();
		new MaxBotClient("test-token", builder.build()).sendMessage(42L, body);
		server.verify();
	}
}
