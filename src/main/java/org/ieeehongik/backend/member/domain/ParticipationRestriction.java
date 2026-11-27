package org.ieeehongik.backend.member.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.ieeehongik.backend.account.domain.Account;

@Entity
@Table(name = "participation_restriction")
public class ParticipationRestriction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ParticipationScope scope;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private RestrictionSource source;

  @Column(nullable = false, length = 500)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RestrictionStatus status;

  @Column(name = "start_at", nullable = false)
  private LocalDateTime startAt;

  @Column(name = "end_at", nullable = false)
  private LocalDateTime endAt;

  @Column(name = "cancelled_at")
  private LocalDateTime cancelledAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  protected ParticipationRestriction() {}

  public ParticipationRestriction(
      Account account,
      ParticipationScope scope,
      RestrictionSource source,
      LocalDateTime startAt,
      LocalDateTime endAt,
      String reason) {
    this.account = Objects.requireNonNull(account);
    this.scope = Objects.requireNonNull(scope);
    this.source = Objects.requireNonNull(source);
    this.startAt = Objects.requireNonNull(startAt);
    this.endAt = Objects.requireNonNull(endAt);

    if (!startAt.isBefore(endAt)) {
      throw new IllegalArgumentException("참여 제한 종료 시각은 시작 시각보다 늦어야 합니다.");
    }

    this.reason = Objects.requireNonNull(reason);
    this.status = RestrictionStatus.ACTIVE;
  }

  public Long getId() {
    return id;
  }

  public Account getAccount() {
    return account;
  }

  public ParticipationScope getScope() {
    return scope;
  }

  public RestrictionSource getSource() {
    return source;
  }

  public LocalDateTime getStartAt() {
    return startAt;
  }

  public LocalDateTime getEndAt() {
    return endAt;
  }

  public String getReason() {
    return reason;
  }

  public RestrictionStatus getStatus() {
    return status;
  }

  public LocalDateTime getCancelledAt() {
    return cancelledAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
