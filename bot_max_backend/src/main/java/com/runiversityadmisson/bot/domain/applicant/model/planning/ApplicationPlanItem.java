package com.runiversityadmisson.bot.domain.applicant.model.planning;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Позиция плана: программа вуза и её место в приоритетах. */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ApplicationPlanItem {

	@Column(name = "university_id", nullable = false, length = 50)
	private String universityId;

	@Column(name = "program_id", nullable = false, length = 80)
	private String programId;

	/** Приоритет вуза в плане, с 0. */
	@Column(name = "university_position", nullable = false)
	private int universityPosition;

	/** Приоритет программы внутри вуза, с 0. */
	@Column(name = "program_position", nullable = false)
	private int programPosition;
}
