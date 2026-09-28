package com.runiversityadmisson.bot.infrastructure.external.max.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NewMessageBody {

    String text;

    @Singular
    List<Attachment> attachments;

    Boolean notify;

    String format;

    public record Attachment(String type, Object payload) {

        public static Attachment inlineKeyboard(List<List<Button>> buttons) {
            return new Attachment("inline_keyboard", new KeyboardPayload(buttons));
        }
    }

    public record KeyboardPayload(List<List<Button>> buttons) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Button(
            String type,
            String text,
            String payload,
            String url,
            @JsonProperty("web_app") String webApp
    ) {

        public Button(String type, String text, String payload, String url) {
            this(type, text, payload, url, null);
        }

        public static Button callback(String text, String payload) {
            return new Button("callback", text, payload, null);
        }

        public static Button link(String text, String url) {
            return new Button("link", text, null, url);
        }

        public static Button openApp(String text, String payload, String webApp) {
            return new Button("open_app", text, payload, null, webApp);
        }

        public static Button requestContact(String text) {
            return new Button("request_contact", text, null, null);
        }

        public static Button requestGeoLocation(String text) {
            return new Button("request_geo_location", text, null, null);
        }

        public static Button message(String text, String payload) {
            return new Button("message", text, payload, null);
        }

        public static Button clipboard(String text, String payload) {
            return new Button("clipboard", text, payload, null);
        }
    }
}
