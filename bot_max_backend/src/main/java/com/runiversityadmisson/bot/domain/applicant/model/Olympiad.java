package com.runiversityadmisson.bot.domain.applicant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Олимпиада: ВсОШ или олимпиада из перечня РСОШ. */
@Entity
@Table(name = "olympiads")
@Getter
@Setter
@NoArgsConstructor
public class Olympiad {

	@Id
	@Column(name = "id", length = 50)
	private String id;

	@Column(name = "name", nullable = false, length = 200)
	private String name;

	@Column(name = "is_vsosh", nullable = false)
	private boolean vsosh;

	/** Номер в перечне олимпиад школьников; у ВсОШ пустой. */
	@Column(name = "list_number")
	private Integer listNumber;

	@OneToMany(mappedBy = "olympiad")
	@OrderBy("profile")
	private List<OlympiadProfile> profiles = new ArrayList<>();
}
