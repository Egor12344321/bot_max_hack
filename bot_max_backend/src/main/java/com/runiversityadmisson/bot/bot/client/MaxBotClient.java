package com.runiversityadmisson.bot.bot.client;

import com.runiversityadmisson.bot.bot.client.dto.NewMessageBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

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
        } catch (RestClientException exception) {
            log.error("Не удалось отправить сообщение пользователю {}", userId, exception);
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
        } catch (RestClientException exception) {
            log.warn("Не удалось подтвердить callback {}", callbackId, exception);
        }
    }
}
