package com.runiversityadmisson.bot.domain.applicant.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "universities")
@Getter
@Setter
@NoArgsConstructor
public class University {

	@Id
	@Column(name = "id", length = 50)
	private String id;

	@Column(name = "name", nullable = false, length = 200)
	private String name;

	@Column(name = "short_name", nullable = false, length = 50)
	private String shortName;

	@Column(name = "city", nullable = false, length = 100)
	private String city;

	/** Сколько баллов за ИД вуз засчитывает в сумме. */
	@Column(name = "achievement_points_max", nullable = false)
	private Integer achievementPointsMax;

	/** id достижения → баллы. Достижения, которых здесь нет, вуз не учитывает. */
	@ElementCollection
	@CollectionTable(name = "achievement_point_rules", joinColumns = @JoinColumn(name = "university_id"))
	@MapKeyColumn(name = "achievement_id", length = 50)
	@Column(name = "points", nullable = false)
	private Map<String, Integer> achievementPoints = new HashMap<>();
}
