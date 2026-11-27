package org.ieeehongik.backend.member.repository;

import org.ieeehongik.backend.member.domain.MembershipStatus;
import org.ieeehongik.backend.member.domain.SemesterMembership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterMembershipRepository extends JpaRepository<SemesterMembership, Long> {

  boolean existsByAccount_IdAndSemester_IdAndStatus(
      Long accountId, Long semesterId, MembershipStatus status);
}
