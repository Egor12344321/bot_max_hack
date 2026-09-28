package com.runiversityadmisson.bot.domain.applicant.ports;

import com.runiversityadmisson.bot.domain.applicant.model.OlympiadBenefitRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OlympiadBenefitRuleRepository extends JpaRepository<OlympiadBenefitRule, Long> {
}
