package com.runiversityadmisson.bot.domain.applicant.model.direction;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "directions")
@Getter
@Setter
@NoArgsConstructor
public class StudyDirection {

	@Id
	@Column(length = 20)
	private String id;

	@Column(nullable = false, unique = true, length = 10)
	private String code;

	@Column(nullable = false, length = 200)
	private String name;

	@ElementCollection
	@CollectionTable(name = "direction_interest_categories", joinColumns = @JoinColumn(name = "direction_id"))
	@Column(name = "category_id", length = 50)
	private Set<String> interestCategoryIds = new LinkedHashSet<>();
}
