
package org.ieeehongik.backend.member.service;

import org.ieeehongik.backend.member.domain.MembershipStatus;
import org.ieeehongik.backend.member.domain.Semester;
import org.ieeehongik.backend.member.domain.SemesterConfig;
import org.ieeehongik.backend.member.repository.SemesterConfigRepository;
import org.ieeehongik.backend.member.repository.SemesterMembershipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberEligibilityServiceTest {

    @Mock
    private SemesterConfigRepository semesterConfigRepository;

    @Mock
    private SemesterMembershipRepository semesterMembershipRepository;

    @InjectMocks
    private MemberEligibilityService memberEligibilityService;

    private static final Long ACCOUNT_ID = 10L;
    private static final Long CURRENT_SEMESTER_ID = 3L;

    @Test
    @DisplayName("M-01: 현재 학기에 승인된 학회원은 검사를 통과한다")
    void approvedMemberPasses() {
        // given
        mockCurrentSemester(CURRENT_SEMESTER_ID);

        when(semesterMembershipRepository
                .existsByAccount_IdAndSemester_IdAndStatus(
                        ACCOUNT_ID,
                        CURRENT_SEMESTER_ID,
                        MembershipStatus.APPROVED
                ))
                .thenReturn(true);

        // when & then
        assertDoesNotThrow(() ->
                memberEligibilityService
                        .requireCurrentSemesterMember(ACCOUNT_ID)
        );

        verify(semesterMembershipRepository)
                .existsByAccount_IdAndSemester_IdAndStatus(
                        ACCOUNT_ID,
                        CURRENT_SEMESTER_ID,
                        MembershipStatus.APPROVED
                );
    }

    @Test
    @DisplayName("M-02: 현재 학기에 승인되지 않은 계정은 예외가 발생한다")
    void unapprovedMemberThrowsException() {
        // given
        mockCurrentSemester(CURRENT_SEMESTER_ID);

        when(semesterMembershipRepository
                .existsByAccount_IdAndSemester_IdAndStatus(
                        ACCOUNT_ID,
                        CURRENT_SEMESTER_ID,
                        MembershipStatus.APPROVED
                ))
                .thenReturn(false);

        // when
        // TODO(#6): 공통 예외 처리 병합 후 BusinessException 검증으로 변경
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> memberEligibilityService
                        .requireCurrentSemesterMember(ACCOUNT_ID)
        );

        // then
        assertEquals(
                "현재 학기에 승인된 학회원이 아닙니다.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("M-03: 현재 학기 설정이 없으면 예외가 발생한다")
    void missingSemesterConfigThrowsException() {
        // given
        when(semesterConfigRepository.findById(1L))
                .thenReturn(Optional.empty());

        // when
        // TODO(#6): 공통 예외 처리 병합 후 BusinessException 검증으로 변경
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> memberEligibilityService
                        .requireCurrentSemesterMember(ACCOUNT_ID)
        );

        // then
        assertEquals(
                "현재 학기 설정이 없습니다.",
                exception.getMessage()
        );

        verifyNoInteractions(semesterMembershipRepository);
    }

    @Test
    @DisplayName("M-04: 학회원 조회에는 현재 학기 설정의 semester ID를 사용한다")
    void usesCurrentSemesterId() {
        // given
        Long newSemesterId = 7L;

        mockCurrentSemester(newSemesterId);

        when(semesterMembershipRepository
                .existsByAccount_IdAndSemester_IdAndStatus(
                        ACCOUNT_ID,
                        newSemesterId,
                        MembershipStatus.APPROVED
                ))
                .thenReturn(true);

        // when
        assertDoesNotThrow(() ->
                memberEligibilityService
                        .requireCurrentSemesterMember(ACCOUNT_ID)
        );

        // then
        verify(semesterMembershipRepository)
                .existsByAccount_IdAndSemester_IdAndStatus(
                        ACCOUNT_ID,
                        newSemesterId,
                        MembershipStatus.APPROVED
                );
    }

    /**
     * DB 없이 현재 운영 학기 조회 결과를 가정한다.
     */
    private void mockCurrentSemester(Long semesterId) {
        SemesterConfig config = mock(SemesterConfig.class);
        Semester semester = mock(Semester.class);

        when(semesterConfigRepository.findById(1L))
                .thenReturn(Optional.of(config));

        when(config.getCurrentSemester())
                .thenReturn(semester);

        when(semester.getId())
                .thenReturn(semesterId);
    }
}