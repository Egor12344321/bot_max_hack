package com.runiversityadmisson.bot.domain.applicant.ports.benefit;

import com.runiversityadmisson.bot.domain.applicant.model.benefit.Achievement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementRepository extends JpaRepository<Achievement, String> {

	List<Achievement> findAllByOrderBySortOrderAsc();
}
