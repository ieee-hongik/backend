package org.ieeehongik.backend.member.domain;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "semester_config")
public class SemesterConfig {

  private static final Long CONFIG_ID = 1L;

  @Id private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "current_semester_id", nullable = false)
  private Semester currentSemester;

  protected SemesterConfig() {}

  public SemesterConfig(Semester currentSemester) {
    this.id = CONFIG_ID;
    this.currentSemester = Objects.requireNonNull(currentSemester);
  }

  public Long getId() {
    return id;
  }

  public Semester getCurrentSemester() {
    return currentSemester;
  }

  public void changeCurrentSemester(Semester semester) {
    this.currentSemester = Objects.requireNonNull(semester);
  }
}
