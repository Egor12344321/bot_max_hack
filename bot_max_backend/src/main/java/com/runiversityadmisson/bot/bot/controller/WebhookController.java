package com.runiversityadmisson.bot.bot.controller;


import com.runiversityadmisson.bot.bot.service.ProcessUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.max.botapi.client.MaxBotAPI;
import ru.max.botapi.model.Update;

@RestController
@RequiredArgsConstructor
@RequestMapping("/webhook/max")
@Slf4j
public class WebhookController {

	private final ProcessUpdateService processUpdateService;
	private final MaxBotAPI maxBotAPI;

	@PostMapping
	public ResponseEntity<Void> handleUpdate(@RequestBody String body) {
		try {
			Update update = maxBotAPI.serializer().deserialize(body, Update.class);
			processUpdateService.process(update);
		} catch (RuntimeException exception) {
			log.error("Не удалось обработать вебхук", exception);
		}
		return ResponseEntity.ok().build();
	}
}
