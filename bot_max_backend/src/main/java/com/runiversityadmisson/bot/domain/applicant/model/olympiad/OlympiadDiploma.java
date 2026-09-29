package com.runiversityadmisson.bot.domain.applicant.model.olympiad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Диплом олимпиады абитуриента: профиль олимпиады и степень. */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class OlympiadDiploma {

	@Column(name = "profile_id", nullable = false, length = 80)
	private String profileId;

	@Enumerated(EnumType.STRING)
	@Column(name = "degree", nullable = false, length = 10)
	private OlympiadDegree degree;
}
