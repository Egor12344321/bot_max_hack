package com.runiversityadmisson.bot.bot.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class EgeSubjectCatalog {

	private static final List<EgeSubject> SUBJECTS = List.of(
		new EgeSubject("russian", "Русский язык", "Орыс тілі", "Орус тили"),
		new EgeSubject("math-profile", "Математика (профиль)", "Математика (бейіндік)", "Математика (профиль)"),
		new EgeSubject("physics", "Физика", "Физика", "Физика"),
		new EgeSubject("chemistry", "Химия", "Химия", "Химия"),
		new EgeSubject("biology", "Биология", "Биология", "Биология"),
		new EgeSubject("informatics", "Информатика", "Информатика", "Информатика"),
		new EgeSubject("social-studies", "Обществознание", "Қоғамтану", "Коом таануу"),
		new EgeSubject("history", "История", "Тарих", "Тарых"),
		new EgeSubject("geography", "География", "География", "География"),
		new EgeSubject("literature", "Литература", "Әдебиет", "Адабият"),
		new EgeSubject("foreign-language", "Иностранный язык", "Шетел тілі", "Чет тили")
	);

	public List<EgeSubject> all() {
		return SUBJECTS;
	}

	public Optional<EgeSubject> findById(String subjectId) {
		return SUBJECTS.stream().filter(subject -> subject.id().equals(subjectId)).findFirst();
	}
}
