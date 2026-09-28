package com.runiversityadmisson.bot.domain.applicant.ports;

import com.runiversityadmisson.bot.domain.applicant.model.InterestCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestCategoryRepository extends JpaRepository<InterestCategory, String> {
}
