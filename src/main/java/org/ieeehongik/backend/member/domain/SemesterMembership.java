package org.ieeehongik.backend.member.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.ieeehongik.backend.account.domain.Account;

@Entity
@Table(
    name = "semester_membership",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_semester_membership_account_semester",
          columnNames = {"account_id", "semester_id"})
    })
public class SemesterMembership {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @ManyToOne
  @JoinColumn(name = "semester_id", nullable = false)
  private Semester semester;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MembershipStatus status;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @CreationTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  protected SemesterMembership() {}

  public SemesterMembership(Account account, Semester semester) {
    this.account = Objects.requireNonNull(account);
    this.semester = Objects.requireNonNull(semester);
    this.status = MembershipStatus.APPROVED;
  }

  public void approve() {
    this.status = MembershipStatus.APPROVED;
  }

  public void revoke() {
    this.status = MembershipStatus.REVOKED;
  }

  public Long getId() {
    return id;
  }

  public Account getAccount() {
    return account;
  }

  public Semester getSemester() {
    return semester;
  }

  public MembershipStatus getStatus() {
    return status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
