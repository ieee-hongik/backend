package org.ieeehongik.backend.member.repository;

import java.time.LocalDateTime;
import org.ieeehongik.backend.member.domain.ParticipationRestriction;
import org.ieeehongik.backend.member.domain.ParticipationScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipationRestrictionRepository
    extends JpaRepository<ParticipationRestriction, Long> {

  @Query(
      """
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM ParticipationRestriction r
            WHERE r.account.id = :accountId
            AND r.scope = :scope
            AND r.status = RestrictionStatus.ACTIVE
            AND r.startAt <= :now
            AND r.endAt > :now
        """)
  boolean existsActiveRestriction(
      @Param("accountId") Long accountId,
      @Param("scope") ParticipationScope scope,
      @Param("now") LocalDateTime now);
}
