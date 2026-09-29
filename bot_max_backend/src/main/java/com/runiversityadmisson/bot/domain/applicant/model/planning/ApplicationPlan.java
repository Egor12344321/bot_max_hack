package com.runiversityadmisson.bot.domain.applicant.model.planning;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Сохранённый план поступления 5×5 пользователя. */
@Entity
@Table(name = "application_plans")
@Getter
@Setter
@NoArgsConstructor
public class ApplicationPlan {

	@Id
	@Column(name = "user_id")
	private UUID userId;

	/** Растёт на 1 при каждом сохранении. */
	@Column(name = "version", nullable = false)
	private int version;

	/** Где пользователь решил использовать БВИ; null — не выбрано. */
	@Column(name = "bvi_program_id", length = 80)
	private String bviProgramId;

	@Column(name = "saved_at", nullable = false)
	private OffsetDateTime savedAt;

	@ElementCollection
	@CollectionTable(name = "application_plan_items", joinColumns = @JoinColumn(name = "user_id"))
	private List<ApplicationPlanItem> items = new ArrayList<>();
}
