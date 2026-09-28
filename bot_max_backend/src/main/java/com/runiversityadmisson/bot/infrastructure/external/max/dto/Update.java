package com.runiversityadmisson.bot.infrastructure.external.max.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Update {

    @JsonProperty("update_type")
    private String updateType;

    private Long timestamp;

    // bot_started
    private User user;
    private String payload;

    // message_created
    private Message message;

    // message_callback
    private Callback callback;

    // message_chat_created и другие
    @JsonProperty("chat_id")
    private Long chatId;

    @Data
    public static class User {
        @JsonProperty("user_id")
        private Long userId;
        @JsonProperty("first_name")
        private String firstName;
        @JsonProperty("last_name")
        private String lastName;
        private String username;
    }

    @Data
    public static class Message {
        private User sender;
        private Recipient recipient;
        private MessageBody body;
        private Long timestamp;
    }

    @Data
    public static class Recipient {
        @JsonProperty("chat_id")
        private Long chatId;
        @JsonProperty("user_id")
        private Long userId;
    }

    @Data
    public static class MessageBody {
        private String text;
        private String mid;
    }

    @Data
    public static class Callback {
        @JsonProperty("callback_id")
        private String callbackId;
        private String payload;
        private User user;
        private Long timestamp;
    }
}
