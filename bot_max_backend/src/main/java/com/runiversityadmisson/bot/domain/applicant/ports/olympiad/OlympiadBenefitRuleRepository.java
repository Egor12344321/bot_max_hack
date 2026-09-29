package com.runiversityadmisson.bot.domain.applicant.ports.olympiad;

import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadBenefitRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OlympiadBenefitRuleRepository extends JpaRepository<OlympiadBenefitRule, Long> {
}
