package com.runiversityadmisson.bot.domain.applicant.ports;

import com.runiversityadmisson.bot.domain.applicant.model.Achievement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementRepository extends JpaRepository<Achievement, String> {

	List<Achievement> findAllByOrderBySortOrderAsc();
}
