package com.runiversityadmisson.bot.application.onboarding.bot;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import static org.assertj.core.api.Assertions.assertThat;

class LocalizationMessagesTest {

	private static final List<String> MESSAGE_KEYS = List.of(
			"greeting",
			"ask.citizenship",
			"track.eaeu",
			"track.russia",
			"citizenship.RU",
			"track.foreigner",
			"ege.ask.subject",
			"ege.ask.score",
			"ege.score.invalid",
			"ege.more",
			"ege.done",
			"choose.button",
			"button.add",
			"button.done",
			"button.open.app",
			"button.accepted",
			"citizenship.BY",
			"citizenship.KZ",
			"citizenship.KG",
			"citizenship.AM",
			"citizenship.OTHER",
			"subject.russian",
			"subject.math-profile",
			"subject.informatics",
			"subject.physics",
			"subject.chemistry",
			"subject.biology",
			"subject.social-studies",
			"subject.history",
			"language.ru",
			"language.kk",
			"language.ky"
	);

	private final ResourceBundleMessageSource messageSource = messageSource();

	@Test
	void allMessagesArePresentAndNotCorruptedInEverySupportedLocale() {
		for (Locale locale : List.of(Locale.forLanguageTag("ru"), Locale.forLanguageTag("kk"),
				Locale.forLanguageTag("ky"))) {
			for (String key : MESSAGE_KEYS) {
				String value = messageSource.getMessage(key, null, locale);
				assertThat(value)
						.as("message '%s' for locale '%s'", key, locale)
						.isNotBlank()
						.doesNotMatch(".*\\?{3,}.*");
			}
		}
	}

	@Test
	void russianGreetingIsLoadedAsUtf8() {
		assertThat(messageSource.getMessage("greeting", null, Locale.forLanguageTag("ru")))
				.startsWith("Привет!");
	}

	private static ResourceBundleMessageSource messageSource() {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename("messages");
		messageSource.setDefaultEncoding("UTF-8");
		messageSource.setFallbackToSystemLocale(false);
		return messageSource;
	}
}
