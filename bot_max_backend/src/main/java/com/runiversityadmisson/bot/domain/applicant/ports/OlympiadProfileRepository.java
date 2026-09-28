package com.runiversityadmisson.bot.domain.applicant.ports;

import com.runiversityadmisson.bot.domain.applicant.model.OlympiadProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OlympiadProfileRepository extends JpaRepository<OlympiadProfile, String> {
}
