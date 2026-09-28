package com.runiversityadmisson.bot.domain.applicant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Индивидуальное достижение (ИД) из справочника.
 * Сколько баллов оно даёт, решает каждый вуз: см. {@link University#getAchievementPoints()}.
 */
@Entity
@Table(name = "achievements")
@Getter
@Setter
@NoArgsConstructor
public class Achievement {

	@Id
	@Column(name = "id", length = 50)
	private String id;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "description", length = 300)
	private String description;

	/** Из достижений одной группы можно выбрать только одно (например, медаль I или II степени). */
	@Column(name = "exclusive_group", length = 50)
	private String exclusiveGroup;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;
}
