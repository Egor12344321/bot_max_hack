package com.runiversityadmisson.bot.domain.applicant.ports.profile;

import com.runiversityadmisson.bot.domain.applicant.model.profile.Language;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LanguageRepository extends JpaRepository<Language, String> {
}
