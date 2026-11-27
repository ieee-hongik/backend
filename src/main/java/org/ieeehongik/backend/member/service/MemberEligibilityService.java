package org.ieeehongik.backend.member.service;

import lombok.RequiredArgsConstructor;
import org.ieeehongik.backend.member.domain.MembershipStatus;
import org.ieeehongik.backend.member.domain.Semester;
import org.ieeehongik.backend.member.repository.SemesterConfigRepository;
import org.ieeehongik.backend.member.repository.SemesterMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberEligibilityService {

  private final SemesterConfigRepository semesterConfigRepository;
  private final SemesterMembershipRepository semesterMembershipRepository;

  public void requireCurrentSemesterMember(Long accountId) {

    Semester currentSemester =
        semesterConfigRepository
            .findById(1L)
            // TODO(#6): 공통 API 응답 및 전역 예외 처리 구현 PR 병합 후
            // BusinessException(MemberErrorCode.CURRENT_SEMESTER_NOT_CONFIGURED)으로 교체
            .orElseThrow(() -> new IllegalStateException("현재 학기 설정이 없습니다."))
            .getCurrentSemester();

    boolean isApproved =
        semesterMembershipRepository.existsByAccount_IdAndSemester_IdAndStatus(
            accountId, currentSemester.getId(), MembershipStatus.APPROVED);

    if (!isApproved) {
      // TODO(#6): 공통 API 응답 및 전역 예외 처리 구현 PR 병합 후
      // BusinessException(MemberErrorCode.MEMBERSHIP_REQUIRED)으로 교체
      throw new IllegalStateException("현재 학기에 승인된 학회원이 아닙니다.");
    }
  }
}
