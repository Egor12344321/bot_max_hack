package com.runiversityadmisson.bot.domain.applicant.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subjects")
@Getter
@Setter
@NoArgsConstructor
public class Subject {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "name_ru", nullable = false, length = 100)
    private String nameRu;

    @Column(name = "name_kk", length = 100)
    private String nameKk;

    @Column(name = "name_ky", length = 100)
    private String nameKy;

    @Column(name = "min_threshold", nullable = false)
    private Integer minThreshold;
}
