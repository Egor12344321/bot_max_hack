package com.runiversityadmisson.bot.domain.applicant.model.olympiad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Правило вуза: что даёт диплом олимпиады. Пустое поле означает «подходит любое значение».
 * Подробнее о полях — в миграции V10.
 */
@Entity
@Table(name = "olympiad_benefit_rules")
@Getter
@Setter
@NoArgsConstructor
public class OlympiadBenefitRule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** null — правило действует во всех вузах (ВсОШ). */
	@Column(name = "university_id", length = 50)
	private String universityId;

	/** null — на всех направлениях вуза, которым соответствует профиль. */
	@Column(name = "program_id", length = 80)
	private String programId;

	/** null — любая олимпиада профиля. */
	@Column(name = "olympiad_id", length = 50)
	private String olympiadId;

	@Column(name = "profile", nullable = false, length = 50)
	private String profile;

	/** true — правило для ВсОШ, false — для олимпиад из перечня. */
	@Column(name = "vsosh", nullable = false)
	private boolean vsosh;

	/** Худший подходящий уровень: 1 — только I уровень, 3 — любой. null — уровень не важен. */
	@Column(name = "max_level")
	private Integer maxLevel;

	/** null — и победителю, и призёру. */
	@Enumerated(EnumType.STRING)
	@Column(name = "degree", length = 10)
	private OlympiadDegree degree;

	@Enumerated(EnumType.STRING)
	@Column(name = "benefit", nullable = false, length = 20)
	private OlympiadBenefit benefit;

	/** Баллы за ИД, только для {@link OlympiadBenefit#ACHIEVEMENT_POINTS}. */
	@Column(name = "points")
	private Integer points;

	/** Минимальный ЕГЭ по предмету профиля для подтверждения диплома; null — подтверждать не нужно. */
	@Column(name = "min_ege_score")
	private Integer minEgeScore;
}
