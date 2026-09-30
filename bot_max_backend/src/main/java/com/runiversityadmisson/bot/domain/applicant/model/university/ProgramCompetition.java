package com.runiversityadmisson.bot.domain.applicant.model.university;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Данные относятся к конкретному конкурсу, а не ко всем местам программы. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ProgramCompetition {
	@Column(name = "data_source", nullable = false)
	private String dataSource = "demo";
	@Column(name = "seats")
	private Integer seats;
	@Column(name = "passing_score")
	private Integer passingScore;
	@Column(name = "previous_year")
	private Integer previousYear;
}
