package com.runiversityadmisson.bot.domain.applicant.ports.direction;

import com.runiversityadmisson.bot.domain.applicant.model.direction.InterestCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestCategoryRepository extends JpaRepository<InterestCategory, String> {
}
