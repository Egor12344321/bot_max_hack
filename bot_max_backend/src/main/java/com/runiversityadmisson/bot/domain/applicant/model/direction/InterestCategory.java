package com.runiversityadmisson.bot.domain.applicant.model.direction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "interest_categories")
@Getter
@Setter
@NoArgsConstructor
public class InterestCategory {

	@Id
	@Column(name = "id", length = 50)
	private String id;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "icon", nullable = false, length = 20)
	private String icon;
}
