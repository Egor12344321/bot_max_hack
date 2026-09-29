package com.runiversityadmisson.bot.domain.applicant.model.profile;

import com.runiversityadmisson.bot.domain.applicant.model.direction.InterestCategory;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadDiploma;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "max_user_id", nullable = false, unique = true)
    private Long maxUserId;

    @Column(name = "language", length = 5)
    private String language;

    @Column(name = "citizenship", length = 5)
    private String citizenship;

	@Column(name = "track", length = 50)
	private String track;

	@ManyToMany
	@JoinTable(
			name = "user_interest_categories",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "category_id"))
	private Set<InterestCategory> interests = new LinkedHashSet<>();

	/** Выбранные направления в порядке предпочтения; позиция хранится в user_directions.position. */
	@ElementCollection
	@CollectionTable(name = "user_directions", joinColumns = @JoinColumn(name = "user_id"))
	@OrderColumn(name = "position")
	@Column(name = "direction_id", length = 20, nullable = false)
	private List<String> directionIds = new ArrayList<>();

	/** ID индивидуальных достижений из таблицы achievements. */
	@ElementCollection
	@CollectionTable(name = "user_achievements", joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "achievement_id", length = 50)
	private Set<String> achievementIds = new LinkedHashSet<>();

	@ElementCollection
	@CollectionTable(name = "user_olympiad_diplomas", joinColumns = @JoinColumn(name = "user_id"))
	private Set<OlympiadDiploma> olympiadDiplomas = new LinkedHashSet<>();

	/** ID льготных категорий из справочника catalog/privilege-categories.json. */
	@ElementCollection
	@CollectionTable(name = "user_privileges", joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "category_id", length = 50)
	private Set<String> privilegeCategoryIds = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
