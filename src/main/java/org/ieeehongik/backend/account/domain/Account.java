package org.ieeehongik.backend.account.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "account")
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 20)
  private String name;

  @Column(name = "student_number", nullable = false, length = 20, unique = true)
  private String studentNumber;

  @Column(nullable = false, length = 255, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AccountRole role;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  protected Account() {}

  public Account(String name, String studentNumber, String email, String passwordHash) {
    this.name = Objects.requireNonNull(name);
    this.studentNumber = Objects.requireNonNull(studentNumber);
    this.email = Objects.requireNonNull(email);
    this.passwordHash = Objects.requireNonNull(passwordHash);
    this.role = AccountRole.USER;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getStudentNumber() {
    return studentNumber;
  }

  public String getEmail() {
    return email;
  }

  public AccountRole getRole() {
    return role;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
