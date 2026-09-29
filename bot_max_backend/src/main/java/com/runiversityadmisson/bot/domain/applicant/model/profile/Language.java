package com.runiversityadmisson.bot.domain.applicant.model.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "languages")
@Getter
@Setter
@NoArgsConstructor
public class Language {

	@Id
	@Column(name = "code", length = 5)
	private String code;

	@Column(name = "name", nullable = false, length = 50)
	private String name;
}
