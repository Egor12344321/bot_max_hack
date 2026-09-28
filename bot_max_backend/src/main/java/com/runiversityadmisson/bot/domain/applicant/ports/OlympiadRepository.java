package com.runiversityadmisson.bot.domain.applicant.ports;

import com.runiversityadmisson.bot.domain.applicant.model.Olympiad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OlympiadRepository extends JpaRepository<Olympiad, String> {
}
