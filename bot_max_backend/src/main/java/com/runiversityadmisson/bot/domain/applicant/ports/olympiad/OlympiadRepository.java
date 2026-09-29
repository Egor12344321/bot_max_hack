package com.runiversityadmisson.bot.domain.applicant.ports.olympiad;

import com.runiversityadmisson.bot.domain.applicant.model.olympiad.Olympiad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OlympiadRepository extends JpaRepository<Olympiad, String> {
}
