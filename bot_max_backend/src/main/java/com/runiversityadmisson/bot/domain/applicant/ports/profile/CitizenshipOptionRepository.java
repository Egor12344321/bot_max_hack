package com.runiversityadmisson.bot.domain.applicant.ports.profile;

import com.runiversityadmisson.bot.domain.applicant.model.profile.CitizenshipOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CitizenshipOptionRepository extends JpaRepository<CitizenshipOption, String> {
}
