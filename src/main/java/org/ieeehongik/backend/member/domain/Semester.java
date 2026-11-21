package org.ieeehongik.backend.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Entity
@Table(
        name = "semester",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_semester_year_term",
                        columnNames = {"year", "term"}
                )
        }
)
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SemesterTerm term;


    protected Semester() {

    }

    public Semester(Integer year, SemesterTerm term) {
        this.year = java.util.Objects.requireNonNull(year);
        this.term = java.util.Objects.requireNonNull(term);
    }

    public Long getId() {
        return id;
    }

    public Integer getYear() {
        return year;
    }

    public SemesterTerm getTerm() {
        return term;
    }
}
