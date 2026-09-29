package com.runiversityadmisson.bot.domain.applicant.ports.planning;

import com.runiversityadmisson.bot.domain.applicant.model.planning.ApplicationPlan;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationPlanRepository extends JpaRepository<ApplicationPlan, UUID> {

	/** Блокирует строку плана до конца транзакции, чтобы параллельные сохранения шли по очереди. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select plan from ApplicationPlan plan where plan.userId = :userId")
	Optional<ApplicationPlan> findForUpdate(@Param("userId") UUID userId);
}
