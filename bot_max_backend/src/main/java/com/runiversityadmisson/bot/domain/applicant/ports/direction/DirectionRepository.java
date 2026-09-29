package com.runiversityadmisson.bot.domain.applicant.ports.direction;

import com.runiversityadmisson.bot.domain.applicant.model.direction.Direction;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectionRepository extends JpaRepository<Direction, String> {
	@Override
	@EntityGraph(attributePaths = "interestCategoryIds")
	List<Direction> findAll();
}
