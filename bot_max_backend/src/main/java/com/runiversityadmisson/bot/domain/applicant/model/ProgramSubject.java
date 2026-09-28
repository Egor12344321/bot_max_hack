package com.runiversityadmisson.bot.domain.applicant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Предмет ЕГЭ направления. Предметы с одинаковым choiceGroup взаимозаменяемы
 * («информатика или физика»); choiceGroup == null — обязательный предмет.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProgramSubject {

	@Column(name = "subject_id", nullable = false, length = 50)
	private String subjectId;

	@Column(name = "choice_group")
	private Integer choiceGroup;
}
