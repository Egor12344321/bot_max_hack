package com.runiversityadmisson.bot.presentation.controller.webhook;


import com.runiversityadmisson.bot.application.onboarding.bot.ProcessUpdateService;
import com.runiversityadmisson.bot.infrastructure.external.max.dto.Update;
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
public class MaxWebhookController {

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
