package com.runiversityadmisson.bot.domain.applicant.ports.olympiad;

import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OlympiadProfileRepository extends JpaRepository<OlympiadProfile, String> {
}
