package com.runiversityadmisson.bot.domain.applicant.ports.exam;

import com.runiversityadmisson.bot.domain.applicant.model.exam.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, String> {
}
