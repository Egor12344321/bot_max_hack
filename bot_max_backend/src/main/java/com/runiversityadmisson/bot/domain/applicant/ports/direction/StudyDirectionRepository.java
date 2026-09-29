package com.runiversityadmisson.bot.domain.applicant.ports.direction;

import com.runiversityadmisson.bot.domain.applicant.model.direction.StudyDirection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyDirectionRepository extends JpaRepository<StudyDirection, String> {
	@Override
	@EntityGraph(attributePaths = "interestCategoryIds")
	List<StudyDirection> findAll();
}
