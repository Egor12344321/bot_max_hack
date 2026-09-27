package com.runiversityadmisson.bot.web.repository;

import com.runiversityadmisson.bot.web.session.ApplicantSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicantSessionRepository extends JpaRepository<ApplicantSession, UUID> {

	Optional<ApplicantSession> findByMaxUserId(Long maxUserId);

	void deleteByMaxUserId(Long maxUserId);
}
