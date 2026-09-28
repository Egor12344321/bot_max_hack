package com.runiversityadmisson.bot.domain.applicant.ports;

import com.runiversityadmisson.bot.domain.applicant.model.CitizenshipOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CitizenshipOptionRepository extends JpaRepository<CitizenshipOption, String> {
}
