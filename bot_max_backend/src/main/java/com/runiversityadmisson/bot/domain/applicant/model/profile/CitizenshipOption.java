package com.runiversityadmisson.bot.domain.applicant.model.profile;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "citizenship_options")
@Getter
@Setter
@NoArgsConstructor
public class CitizenshipOption {

    @Id
    @Column(name = "code", length = 5)
    private String code;

    @Column(name = "name_ru", nullable = false, length = 100)
    private String nameRu;

    @Column(name = "name_kk", length = 100)
    private String nameKk;

    @Column(name = "name_ky", length = 100)
    private String nameKy;

    @Column(name = "group_name", nullable = false, length = 20)
    private String groupName;
}
