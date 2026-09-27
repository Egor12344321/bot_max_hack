package com.runiversityadmisson.bot.bot.client.dto;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class NewMessageBody {

    String text;

    @Singular
    List<Attachment> attachments;

    Boolean disableLinkPreview;

    Boolean notify;

    String format;

    public record Attachment(String type, Object payload) {

        public static Attachment inlineKeyboard(List<List<Button>> buttons) {
            return new Attachment("inline_keyboard", new KeyboardPayload(buttons));
        }
    }

    public record KeyboardPayload(List<List<Button>> buttons) {}

    public record Button(
            String type,
            String text,
            String payload,
            String url
    ) {

        public static Button callback(String text, String payload) {
            return new Button("callback", text, payload, null);
        }

        public static Button link(String text, String url) {
            return new Button("link", text, null, url);
        }

        public static Button openApp(String text, String payload) {
            return new Button("open_app", text, payload, null);
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
