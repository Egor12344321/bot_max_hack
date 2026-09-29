package com.runiversityadmisson.bot.domain.applicant.model.university;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Направление (образовательная программа) вуза. */
@Entity
@Table(name = "programs")
@Getter
@Setter
@NoArgsConstructor
public class Program {

	@Id
	@Column(name = "id", length = 80)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "university_id", nullable = false)
	private University university;

	@Column(name = "code", length = 10)
	private String code;

	@Column(name = "name", nullable = false, length = 200)
	private String name;

	@Column(name = "passing_score_previous_year")
	private Integer passingScorePreviousYear;

	@ElementCollection
	@CollectionTable(name = "program_subjects", joinColumns = @JoinColumn(name = "program_id"))
	private List<ProgramSubject> subjects = new ArrayList<>();

	/** Профили олимпиад, которые вуз считает соответствующими направлению. */
	@ElementCollection
	@CollectionTable(name = "program_olympiad_profiles", joinColumns = @JoinColumn(name = "program_id"))
	@Column(name = "profile", length = 50)
	private Set<String> olympiadProfiles = new LinkedHashSet<>();
}
