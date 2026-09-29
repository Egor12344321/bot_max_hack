package com.runiversityadmisson.bot.domain.applicant.ports.profile;

import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByMaxUserId(Long maxUserId);

	void deleteByMaxUserId(Long maxUserId);
}
