package com.runiversityadmisson.bot.bot.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.model.AttachmentRequest;
import ru.max.botapi.model.Button;
import ru.max.botapi.model.ButtonIntent;
import ru.max.botapi.model.CallbackAnswer;
import ru.max.botapi.model.CallbackButton;
import ru.max.botapi.model.InlineKeyboardAttachment.KeyboardPayload;
import ru.max.botapi.model.InlineKeyboardAttachmentRequest;
import ru.max.botapi.model.NewMessageBody;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaxBotService {

	private final MaxBotAPI maxBotAPI;

	public void sendQuestion(long userId, BotQuestion question) {
		try {
			List<AttachmentRequest> attachments = question.buttons().isEmpty()
				? List.of()
				: List.of(new InlineKeyboardAttachmentRequest(new KeyboardPayload(keyboard(question.buttons()))));
			NewMessageBody message = new NewMessageBody(question.text(), attachments, null, true, null);
			maxBotAPI.sendMessage(message).userId(userId).execute();
		} catch (RuntimeException exception) {
			log.error("Не удалось отправить сообщение", exception);
		}
	}

	public void sendText(long userId, String text) {
		sendQuestion(userId, new BotQuestion(text, List.of()));
	}

	public void answerCallback(String callbackId) {
		try {
			maxBotAPI.answerOnCallback(new CallbackAnswer(null, "Принято"), callbackId).execute();
		} catch (RuntimeException exception) {
			log.warn("Не удалось подтвердить кнопку");
		}
	}

	private List<List<Button>> keyboard(List<List<BotButton>> rows) {
		return rows.stream()
			.<List<Button>>map(row -> row.stream()
				.map(button -> (Button) new CallbackButton(button.text(), button.payload(), ButtonIntent.DEFAULT))
				.toList())
			.toList();
	}
}
