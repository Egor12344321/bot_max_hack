package com.runiversityadmisson.bot.bot.controller;


import com.runiversityadmisson.bot.bot.model.Update;
import com.runiversityadmisson.bot.bot.service.ProcessUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequiredArgsConstructor
@RequestMapping("/webhook/max")
@Slf4j
public class WebhookController {

	private final ProcessUpdateService processUpdateService;
	private final ObjectMapper objectMapper;

	@PostMapping
	public ResponseEntity<Void> handleUpdate(@RequestBody String body) {
		try {
			Update update = objectMapper.readValue(body, Update.class);
			log.debug("Webhook: update_type={}", update.getUpdateType());
			processUpdateService.process(update);
		} catch (Exception exception) {
			log.error("Webhook: не удалось обработать", exception);
		}
		return ResponseEntity.ok().build();
	}
}
