package com.runiversityadmisson.bot.domain.applicant.ports;

import com.runiversityadmisson.bot.domain.applicant.model.Program;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, String> {

	List<Program> findAllByOrderByUniversityIdAscIdAsc();

	List<Program> findByUniversityIdOrderByIdAsc(String universityId);
}
