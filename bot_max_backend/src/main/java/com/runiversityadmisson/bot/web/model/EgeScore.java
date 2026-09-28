package com.runiversityadmisson.bot.web.model;


import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ege_scores",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "subject_id"}))
@Getter
@Setter
@NoArgsConstructor
public class EgeScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "subject_id", nullable = false, length = 50)
    private String subjectId;

    @Column(name = "score", nullable = false)
    private Integer score;
}