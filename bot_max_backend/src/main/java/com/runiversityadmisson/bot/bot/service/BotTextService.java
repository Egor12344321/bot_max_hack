package com.runiversityadmisson.bot.bot.service;

import com.runiversityadmisson.bot.bot.session.BotSession;
import com.runiversityadmisson.bot.bot.session.SessionState;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BotTextService {

	private final EgeSubjectCatalog egeSubjectCatalog;

	public BotQuestion questionFor(BotSession session) {
		return switch (session.state()) {
			case NEW, WAITING_FOR_LANGUAGE -> languageQuestion();
			case WAITING_FOR_CITIZENSHIP -> citizenshipQuestion(session.language());
			case WAITING_FOR_TRACK -> trackQuestion(session.language());
			case WAITING_FOR_EGE_SUBJECT -> egeSubjectQuestion(session);
			case WAITING_FOR_EGE_SCORE -> egeScoreQuestion(session);
			case READY_FOR_MINAPP -> readyQuestion(session.language());
		};
	}

	public String invalidEge(String language) {
		return switch (language(language)) {
			case "kk" -> "0-ден 100-ге дейін бір балл енгізіңіз.";
			case "ky" -> "0дөн 100гө чейин бир балл киргизиңиз.";
			default -> "Введите один балл от 0 до 100.";
		};
	}

	private BotQuestion languageQuestion() {
		return new BotQuestion(
			"Выберите язык / Тілді таңдаңыз / Тилди тандаңыз",
			List.of(List.of(
				new BotButton("Русский", "lang_ru"),
				new BotButton("Қазақша", "lang_kk"),
				new BotButton("Кыргызча", "lang_ky")
			))
		);
	}

	private BotQuestion citizenshipQuestion(String language) {
		return switch (language(language)) {
			case "kk" -> new BotQuestion("Азаматтық тобын таңдаңыз", List.of(List.of(
				new BotButton("ЕАЭО", "citizenship_eaeu"), new BotButton("Басқа ел", "citizenship_other")
			)));
			case "ky" -> new BotQuestion("Жарандык тобун тандаңыз", List.of(List.of(
				new BotButton("ЕАЭБ", "citizenship_eaeu"), new BotButton("Башка өлкө", "citizenship_other")
			)));
			default -> new BotQuestion("Выберите группу гражданства", List.of(List.of(
				new BotButton("ЕАЭС", "citizenship_eaeu"), new BotButton("Другая страна", "citizenship_other")
			)));
		};
	}

	private BotQuestion trackQuestion(String language) {
		return switch (language(language)) {
			case "kk" -> new BotQuestion("Оқу түрін таңдаңыз", List.of(List.of(
				new BotButton("Грант", "track_budget"), new BotButton("Ақылы", "track_paid")
			)));
			case "ky" -> new BotQuestion("Окуу түрүн тандаңыз", List.of(List.of(
				new BotButton("Бюджет", "track_budget"), new BotButton("Келишим", "track_paid")
			)));
			default -> new BotQuestion("Выберите вариант поступления", List.of(List.of(
				new BotButton("Бюджет", "track_budget"), new BotButton("Контракт", "track_paid")
			)));
		};
	}

	private BotQuestion egeSubjectQuestion(BotSession session) {
		List<List<BotButton>> buttons = new ArrayList<>();
		List<BotButton> row = new ArrayList<>(2);
		for (EgeSubject subject : egeSubjectCatalog.all()) {
			if (session.egeScores().containsKey(subject.id())) {
				continue;
			}
			row.add(new BotButton(subject.name(session.language()), "subject_" + subject.id()));
			if (row.size() == 2) {
				buttons.add(List.copyOf(row));
				row.clear();
			}
		}
		if (!row.isEmpty()) {
			buttons.add(List.copyOf(row));
		}
		if (!session.egeScores().isEmpty()) {
			buttons.add(List.of(new BotButton(doneButton(session.language()), "ege_done")));
		}
		return new BotQuestion(subjectQuestion(session.language()), List.copyOf(buttons));
	}

	private BotQuestion egeScoreQuestion(BotSession session) {
		return egeSubjectCatalog.findById(session.selectedSubjectId())
			.map(subject -> new BotQuestion(scoreQuestion(session.language(), subject.name(session.language())), List.of()))
			.orElseGet(() -> egeSubjectQuestion(session));
	}

	private BotQuestion readyQuestion(String language) {
		return switch (language(language)) {
			case "kk" -> new BotQuestion("Деректер сақталды. Мини-аппқа оралыңыз.", List.of());
			case "ky" -> new BotQuestion("Маалыматтар сакталды. Мини-аппка кайтыңыз.", List.of());
			default -> new BotQuestion("Данные сохранены. Вернитесь в мини-приложение.", List.of());
		};
	}

	private String language(String language) {
		return switch (language) {
			case "kk", "ky" -> language;
			default -> "ru";
		};
	}

	private String subjectQuestion(String language) {
		return switch (language(language)) {
			case "kk" -> "Пәнді таңдаңыз. Ең көбі 5 пән.";
			case "ky" -> "Предметти тандаңыз. Эң көп 5 предмет.";
			default -> "Выберите предмет. Можно указать до 5 предметов.";
		};
	}

	private String scoreQuestion(String language, String subject) {
		return switch (language(language)) {
			case "kk" -> "" + subject + " бойынша баллды енгізіңіз";
			case "ky" -> subject + " боюнча баллды киргизиңиз";
			default -> "Введите балл по предмету: " + subject;
		};
	}

	private String doneButton(String language) {
		return switch (language(language)) {
			case "kk" -> "Дайын";
			case "ky" -> "Даяр";
			default -> "Готово";
		};
	}
}
