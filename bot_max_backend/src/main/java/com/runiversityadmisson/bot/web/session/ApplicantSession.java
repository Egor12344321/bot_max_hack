package com.runiversityadmisson.bot.web.session;

import com.runiversityadmisson.bot.bot.session.SessionState;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "applicant_sessions")
@Getter
@Setter
public class ApplicantSession {

	@Id
	private UUID id;

	@Column(name = "max_user_id", nullable = false, unique = true)
	private Long maxUserId;

	@Column(nullable = false, length = 2)
	private String language;

	@Column(name = "country_code", length = 16)
	private String countryCode;

	@Column(nullable = false, length = 32)
	private String track;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private SessionState state;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "applicant_session_ege_scores", joinColumns = @JoinColumn(name = "session_id"))
	@MapKeyColumn(name = "subject_id", length = 64)
	@Column(name = "score", nullable = false)
	private Map<String, Integer> egeScores = new HashMap<>();

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;
}
