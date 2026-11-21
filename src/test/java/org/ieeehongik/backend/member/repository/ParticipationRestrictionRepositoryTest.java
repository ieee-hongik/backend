
package org.ieeehongik.backend.member.repository;

import jakarta.persistence.EntityManager;
import org.ieeehongik.backend.account.domain.Account;
import org.ieeehongik.backend.member.domain.ParticipationRestriction;
import org.ieeehongik.backend.member.domain.ParticipationScope;
import org.ieeehongik.backend.member.domain.RestrictionSource;
import org.ieeehongik.backend.member.domain.RestrictionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Testcontainers
class ParticipationRestrictionRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql =
            new MySQLContainer("mysql:8.4");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ParticipationRestrictionRepository repository;

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 11, 21, 12, 0);

    @Test
    @DisplayName("RR-01: 참여 제한 기록이 없으면 false")
    void noRestrictionReturnsFalse() {
        Account account = persistAccount("20260001");
        flushAndClear();

        assertFalse(existsAt(account, ParticipationScope.PROJECT, NOW));
    }

    @Test
    @DisplayName("RR-02: PROJECT에 유효한 제한이 있으면 true")
    void activeProjectRestrictionReturnsTrue() {
        Account account = persistAccount("20260002");

        persistRestriction(
                account,
                ParticipationScope.PROJECT,
                NOW.minusDays(1),
                NOW.plusDays(1)
        );
        flushAndClear();

        assertTrue(existsAt(account, ParticipationScope.PROJECT, NOW));
    }

    @Test
    @DisplayName("RR-03: PROJECT 제한은 STUDY 검사에 적용되지 않는다")
    void differentScopeReturnsFalse() {
        Account account = persistAccount("20260003");

        persistRestriction(
                account,
                ParticipationScope.PROJECT,
                NOW.minusDays(1),
                NOW.plusDays(1)
        );
        flushAndClear();

        assertFalse(existsAt(account, ParticipationScope.STUDY, NOW));
    }

    @Test
    @DisplayName("RR-04: CANCELLED 상태의 제한은 적용되지 않는다")
    void cancelledRestrictionReturnsFalse() {
        Account account = persistAccount("20260004");

        ParticipationRestriction restriction = persistRestriction(
                account,
                ParticipationScope.PROJECT,
                NOW.minusDays(1),
                NOW.plusDays(1)
        );

        // cancel()은 아직 구현하지 않았으므로
        // 테스트 데이터의 상태를 JPQL로 직접 변경한다.
        entityManager.flush();

        entityManager.createQuery("""
                UPDATE ParticipationRestriction r
                SET r.status = :status,
                    r.cancelledAt = :cancelledAt
                WHERE r.id = :id
                """)
                .setParameter("status", RestrictionStatus.CANCELLED)
                .setParameter("cancelledAt", NOW.minusHours(1))
                .setParameter("id", restriction.getId())
                .executeUpdate();

        entityManager.clear();

        assertFalse(existsAt(account, ParticipationScope.PROJECT, NOW));
    }

    @Test
    @DisplayName("RR-05: 다른 계정의 제한은 적용되지 않는다")
    void anotherAccountsRestrictionReturnsFalse() {
        Account targetAccount = persistAccount("20260005");
        Account restrictedAccount = persistAccount("20260006");

        persistRestriction(
                restrictedAccount,
                ParticipationScope.PROJECT,
                NOW.minusDays(1),
                NOW.plusDays(1)
        );
        flushAndClear();

        assertFalse(
                existsAt(targetAccount, ParticipationScope.PROJECT, NOW)
        );
    }

    @Test
    @DisplayName("RR-06: 시작 시각 이전에는 제한되지 않는다")
    void beforeStartReturnsFalse() {
        Account account = persistAccount("20260007");

        persistRestriction(
                account,
                ParticipationScope.PROJECT,
                NOW.plusHours(1),
                NOW.plusDays(1)
        );
        flushAndClear();

        assertFalse(existsAt(account, ParticipationScope.PROJECT, NOW));
    }

    @Test
    @DisplayName("RR-07: 시작 시각과 같으면 제한된다")
    void atStartReturnsTrue() {
        Account account = persistAccount("20260008");

        persistRestriction(
                account,
                ParticipationScope.PROJECT,
                NOW,
                NOW.plusDays(1)
        );
        flushAndClear();

        assertTrue(existsAt(account, ParticipationScope.PROJECT, NOW));
    }

    @Test
    @DisplayName("RR-08: 제한 기간 중에는 제한된다")
    void duringRestrictionReturnsTrue() {
        Account account = persistAccount("20260009");

        persistRestriction(
                account,
                ParticipationScope.PROJECT,
                NOW.minusHours(1),
                NOW.plusHours(1)
        );
        flushAndClear();

        assertTrue(existsAt(account, ParticipationScope.PROJECT, NOW));
    }

    @Test
    @DisplayName("RR-09: 종료 시각과 같으면 제한되지 않는다")
    void atEndReturnsFalse() {
        Account account = persistAccount("20260010");

        persistRestriction(
                account,
                ParticipationScope.PROJECT,
                NOW.minusDays(1),
                NOW
        );
        flushAndClear();

        assertFalse(existsAt(account, ParticipationScope.PROJECT, NOW));
    }

    @Test
    @DisplayName("RR-10: 종료 시각 이후에는 제한되지 않는다")
    void afterEndReturnsFalse() {
        Account account = persistAccount("20260011");

        persistRestriction(
                account,
                ParticipationScope.PROJECT,
                NOW.minusDays(2),
                NOW.minusDays(1)
        );
        flushAndClear();

        assertFalse(existsAt(account, ParticipationScope.PROJECT, NOW));
    }

    @Test
    @DisplayName("RR-11: 제한 기록이 여러 개라도 유효한 기록이 하나 있으면 true")
    void anyActiveRestrictionReturnsTrue() {
        Account account = persistAccount("20260012");

        // 이미 만료된 제한
        persistRestriction(
                account,
                ParticipationScope.PROJECT,
                NOW.minusDays(3),
                NOW.minusDays(2)
        );

        // 현재 유효한 제한
        persistRestriction(
                account,
                ParticipationScope.PROJECT,
                NOW.minusHours(1),
                NOW.plusHours(1)
        );
        flushAndClear();

        assertTrue(existsAt(account, ParticipationScope.PROJECT, NOW));
    }

    // 테스트 데이터 생성

    private Account persistAccount(String studentNumber) {
        Account account = new Account(
                "테스트회원",
                studentNumber,
                studentNumber + "@example.com",
                "test-password-hash"
        );

        entityManager.persist(account);
        return account;
    }

    private ParticipationRestriction persistRestriction(
            Account account,
            ParticipationScope scope,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        ParticipationRestriction restriction =
                new ParticipationRestriction(
                        account,
                        scope,
                        RestrictionSource.ADMIN,
                        startAt,
                        endAt,
                        "통합 테스트용 참여 제한"
                );

        entityManager.persist(restriction);
        return restriction;
    }

    // Repository 호출

    private boolean existsAt(
            Account account,
            ParticipationScope scope,
            LocalDateTime now
    ) {
        return repository.existsActiveRestriction(
                account.getId(),
                scope,
                now
        );
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}