package com.runiversityadmisson.bot.application.max.onboarding;

import com.runiversityadmisson.bot.infrastructure.external.max.MaxBotClient;
import com.runiversityadmisson.bot.infrastructure.external.max.dto.NewMessageBody;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/**
 * Отправка сообщений бота: тексты, кнопки, локализация.
 */
@Service
@Slf4j
public class OnboardingMessenger {

    private final MaxBotClient maxBotClient;
    private final MessageSource messageSource;
    private final String webApp;

    public OnboardingMessenger(MaxBotClient maxBotClient,
                             MessageSource messageSource,
                             @Value("${max.bot.web-app}") String webApp) {
        this.maxBotClient = maxBotClient;
        this.messageSource = messageSource;
        this.webApp = webApp;
    }

    public void sendLanguageQuestion(Long userId) {
        maxBotClient.sendMessage(userId, NewMessageBody.builder()
                .text(msg("greeting", "ru"))
                .attachment(NewMessageBody.Attachment.inlineKeyboard(List.of(
                        List.of(
                                NewMessageBody.Button.callback(msg("language.ru", "ru"), "lang_ru"),
                                NewMessageBody.Button.callback(msg("language.kk", "kk"), "lang_kk")
                        ),
                        List.of(
                                NewMessageBody.Button.callback(msg("language.ky", "ky"), "lang_ky")
                        )
                )))
                .build());
    }

    public void sendCitizenshipQuestion(Long userId, String lang) {
        maxBotClient.sendMessage(userId, NewMessageBody.builder()
                .text(msg("ask.citizenship", lang))
                .attachment(NewMessageBody.Attachment.inlineKeyboard(List.of(
                        List.of(NewMessageBody.Button.callback(msg("citizenship.RU", lang), "citizenship_RU")),
                        List.of(
                                NewMessageBody.Button.callback(msg("citizenship.BY", lang), "citizenship_BY"),
                                NewMessageBody.Button.callback(msg("citizenship.KZ", lang), "citizenship_KZ")
                        ),
                        List.of(
                                NewMessageBody.Button.callback(msg("citizenship.KG", lang), "citizenship_KG"),
                                NewMessageBody.Button.callback(msg("citizenship.AM", lang), "citizenship_AM")
                        ),
                        List.of(
                                NewMessageBody.Button.callback(msg("citizenship.OTHER", lang), "citizenship_OTHER")
                        )
                )))
                .build());
    }

    public void sendSubjectQuestion(Long userId, String lang) {
        maxBotClient.sendMessage(userId, NewMessageBody.builder()
                .text(msg("ege.ask.subject", lang))
                .attachment(NewMessageBody.Attachment.inlineKeyboard(List.of(
                        List.of(
                                NewMessageBody.Button.callback(msg("subject.russian", lang), "subject_russian"),
                                NewMessageBody.Button.callback(msg("subject.math-profile", lang), "subject_math-profile")
                        ),
                        List.of(
                                NewMessageBody.Button.callback(msg("subject.informatics", lang), "subject_informatics"),
                                NewMessageBody.Button.callback(msg("subject.physics", lang), "subject_physics")
                        ),
                        List.of(
                                NewMessageBody.Button.callback(msg("subject.chemistry", lang), "subject_chemistry"),
                                NewMessageBody.Button.callback(msg("subject.biology", lang), "subject_biology")
                        ),
                        List.of(
                                NewMessageBody.Button.callback(msg("subject.social-studies", lang), "subject_social-studies"),
                                NewMessageBody.Button.callback(msg("subject.history", lang), "subject_history")
                        )
                )))
                .build());
    }

    public void sendMoreSubjectsQuestion(Long userId, String lang) {
        maxBotClient.sendMessage(userId, NewMessageBody.builder()
                .text(msg("ege.more", lang))
                .attachment(NewMessageBody.Attachment.inlineKeyboard(List.of(
                        List.of(
                                NewMessageBody.Button.callback(msg("button.add", lang), "ege_more"),
                                NewMessageBody.Button.callback(msg("button.done", lang), "ege_done")
                        )
                )))
                .build());
    }

    public void sendEgeScoreQuestion(Long userId, String lang) {
        maxBotClient.sendMessage(userId, NewMessageBody.builder()
                .text(msg("ege.ask.score", lang))
                .build());
    }

    public void sendInvalidScore(Long userId, String lang) {
        maxBotClient.sendMessage(userId, NewMessageBody.builder()
                .text(msg("ege.score.invalid", lang))
                .build());
    }

    public void sendChooseButtonHint(Long userId, String lang) {
        maxBotClient.sendMessage(userId, NewMessageBody.builder()
                .text(msg("choose.button", lang))
                .build());
    }

    /** Приветствие, когда язык и гражданство не спрашиваются. */
    public void sendEgeGreeting(Long userId, String lang) {
        maxBotClient.sendMessage(userId, NewMessageBody.builder()
                .text(msg("greeting.ege", lang))
                .build());
    }

    public void sendTrackMessage(Long userId, String lang, String trackKey) {
        maxBotClient.sendMessage(userId, NewMessageBody.builder()
                .text(msg(trackKey, lang))
                .build());
    }

    public void sendForeignerInfo(Long userId, String lang) {
        maxBotClient.sendMessage(userId, NewMessageBody.builder()
                .text(msg("track.foreigner", lang))
                .attachment(openMiniAppButton(lang))
                .build());
    }

    public void sendEgeDone(Long userId, String lang) {
        maxBotClient.sendMessage(userId, NewMessageBody.builder()
                .text(msg("ege.done", lang))
                .attachment(openMiniAppButton(lang))
                .build());
    }

    private NewMessageBody.Attachment openMiniAppButton(String lang) {
		log.info("MAX: формируется кнопка открытия mini-app, webApp={}", webApp);
        return NewMessageBody.Attachment.inlineKeyboard(List.of(
                List.of(NewMessageBody.Button.openApp(
                        msg("button.open.app", lang),
                        null, webApp
                ))
        ));
    }

    private String msg(String key, String lang) {
        Locale locale = switch (lang == null ? "ru" : lang) {
            case "kk" -> Locale.forLanguageTag("kk");
            case "ky" -> Locale.forLanguageTag("ky");
            default -> Locale.forLanguageTag("ru");
        };
        return messageSource.getMessage(key, null, locale);
    }
}
