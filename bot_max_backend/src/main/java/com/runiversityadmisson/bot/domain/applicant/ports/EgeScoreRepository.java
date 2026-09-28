package com.runiversityadmisson.bot.domain.applicant.ports;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.runiversityadmisson.bot.domain.applicant.model.EgeScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EgeScoreRepository extends JpaRepository<EgeScore, UUID> {

    List<EgeScore> findByUserId(UUID userId);

    Optional<EgeScore> findByUserIdAndSubjectId(UUID userId, String subjectId);

    @Modifying
    @Query("DELETE FROM EgeScore e WHERE e.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
