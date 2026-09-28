package com.runiversityadmisson.bot.web.repository;

import com.runiversityadmisson.bot.web.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, String> {
}
