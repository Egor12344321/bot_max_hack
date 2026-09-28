package com.runiversityadmisson.bot.infrastructure.external.max;

import com.runiversityadmisson.bot.infrastructure.external.max.dto.NewMessageBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.List;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Component
@Slf4j
public class MaxBotClient {

    private final RestClient restClient;
    private final String token;

    @Autowired
    public MaxBotClient(@Value("${max.bot.token}") String token) {
        this(token, RestClient.builder().baseUrl("https://platform-api2.max.ru").build());
    }

    MaxBotClient(String token, RestClient restClient) {
        this.token = token;
        this.restClient = restClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerCommands() {
        if (token.isBlank()) {
            return;
        }
        try {
            restClient.patch()
                    .uri("/me/commands")
                    .header("Authorization", token)
                    .header("Content-Type", "application/json")
                    .body(Map.of("commands", List.of(
                            Map.of("name", "restart", "description", "Начать заново")
                    )))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Команда /restart зарегистрирована в MAX");
        } catch (RestClientException exception) {
            log.warn("Не удалось зарегистрировать команду /restart: {}", exception.getClass().getSimpleName());
        }
    }

    public void sendMessage(long userId, NewMessageBody body) {
        try {
            restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/messages")
                            .queryParam("user_id", userId)
                            .build())
                    .header("Authorization", token)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
			log.debug("MAX: сообщение принято API");
        } catch (RestClientException exception) {
			log.error("MAX: не удалось отправить сообщение, причина={}", exception.getClass().getSimpleName());
        }
    }

    public void answerCallback(String callbackId, String notification) {
        try {
            restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/answers")
                            .queryParam("callback_id", callbackId).build())
                    .header("Authorization", token)
                    .header("Content-Type", "application/json")
                    .body(Map.of(
                            "notification", notification
                    ))
                    .retrieve()
                    .toBodilessEntity();
			log.debug("MAX: callback подтверждён API");
        } catch (RestClientException exception) {
			log.warn("MAX: не удалось подтвердить callback, причина={}", exception.getClass().getSimpleName());
        }
    }
}
