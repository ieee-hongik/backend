package org.ieeehongik.backend.member.repository;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;
import org.ieeehongik.backend.account.domain.Account;
import org.ieeehongik.backend.member.domain.MembershipStatus;
import org.ieeehongik.backend.member.domain.Semester;
import org.ieeehongik.backend.member.domain.SemesterMembership;
import org.ieeehongik.backend.member.domain.SemesterTerm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
class SemesterMembershipRepositoryTest {

  @Container @ServiceConnection static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

  @Autowired private EntityManager entityManager;

  @Autowired private SemesterMembershipRepository repository;

  @Test
  @DisplayName("MR-01: 해당 학기에 APPROVED 기록이 있으면 true")
  void approvedMembershipExists() {
    // given
    Account account = persistAccount("20260001");
    Semester semester = persistSemester(2026, SemesterTerm.FIRST);

    entityManager.persist(new SemesterMembership(account, semester));
    flushAndClear();

    // when
    boolean result =
        repository.existsByAccount_IdAndSemester_IdAndStatus(
            account.getId(), semester.getId(), MembershipStatus.APPROVED);

    // then
    assertTrue(result);
  }

  @Test
  @DisplayName("MR-02: REVOKED 상태라면 APPROVED 조회 결과는 false")
  void revokedMembershipDoesNotQualify() {
    // given
    Account account = persistAccount("20260002");
    Semester semester = persistSemester(2026, SemesterTerm.FIRST);

    SemesterMembership membership = new SemesterMembership(account, semester);

    membership.revoke();

    entityManager.persist(membership);
    flushAndClear();

    // when
    boolean result =
        repository.existsByAccount_IdAndSemester_IdAndStatus(
            account.getId(), semester.getId(), MembershipStatus.APPROVED);

    // then
    assertFalse(result);
  }

  @Test
  @DisplayName("MR-03: 학회원 자격 기록이 없으면 false")
  void missingMembershipDoesNotQualify() {
    // given
    Account account = persistAccount("20260003");
    Semester semester = persistSemester(2026, SemesterTerm.FIRST);
    flushAndClear();

    // when
    boolean result =
        repository.existsByAccount_IdAndSemester_IdAndStatus(
            account.getId(), semester.getId(), MembershipStatus.APPROVED);

    // then
    assertFalse(result);
  }

  @Test
  @DisplayName("MR-04: 이전 학기에만 승인된 계정은 현재 학기 조회에서 false")
  void previousSemesterApprovalDoesNotQualify() {
    // given
    Account account = persistAccount("20260004");

    Semester previousSemester = persistSemester(2026, SemesterTerm.FIRST);

    Semester currentSemester = persistSemester(2026, SemesterTerm.SECOND);

    entityManager.persist(new SemesterMembership(account, previousSemester));
    flushAndClear();

    // when
    boolean result =
        repository.existsByAccount_IdAndSemester_IdAndStatus(
            account.getId(), currentSemester.getId(), MembershipStatus.APPROVED);

    // then
    assertFalse(result);
  }

  @Test
  @DisplayName("MR-05: 다른 계정만 승인되어 있으면 false")
  void anotherAccountsApprovalDoesNotQualify() {
    // given
    Account targetAccount = persistAccount("20260005");
    Account approvedAccount = persistAccount("20260006");

    Semester semester = persistSemester(2026, SemesterTerm.FIRST);

    entityManager.persist(new SemesterMembership(approvedAccount, semester));
    flushAndClear();

    // when
    boolean result =
        repository.existsByAccount_IdAndSemester_IdAndStatus(
            targetAccount.getId(), semester.getId(), MembershipStatus.APPROVED);

    // then
    assertFalse(result);
  }

  private Account persistAccount(String studentNumber) {
    Account account =
        new Account("테스트회원", studentNumber, studentNumber + "@example.com", "test-password-hash");

    entityManager.persist(account);
    return account;
  }

  private Semester persistSemester(Integer year, SemesterTerm term) {
    Semester semester = new Semester(year, term);
    entityManager.persist(semester);
    return semester;
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
