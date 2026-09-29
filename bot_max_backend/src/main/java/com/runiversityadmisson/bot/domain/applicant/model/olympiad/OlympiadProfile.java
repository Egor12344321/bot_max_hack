package com.runiversityadmisson.bot.domain.applicant.model.olympiad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Профиль олимпиады в конкретном учебном году.
 * Уровень I–III присваивается профилю, а не олимпиаде: у «Физтеха» физика I уровня, а математика II.
 */
@Entity
@Table(name = "olympiad_profiles")
@Getter
@Setter
@NoArgsConstructor
public class OlympiadProfile {

	@Id
	@Column(name = "id", length = 80)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "olympiad_id", nullable = false)
	private Olympiad olympiad;

	/** Код профиля: math, informatics, physics, ai. По нему вуз сопоставляет олимпиаду с направлением. */
	@Column(name = "profile", nullable = false, length = 50)
	private String profile;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	/** Предмет ЕГЭ, которым подтверждают диплом и по которому дают 100 баллов. */
	@Column(name = "subject_id", nullable = false, length = 50)
	private String subjectId;

	/** 1–3; у ВсОШ уровня нет. */
	@Column(name = "level")
	private Integer level;

	/** Год заключительного этапа: от него считается срок действия диплома. */
	@Column(name = "olympiad_year", nullable = false)
	private Integer olympiadYear;
}
