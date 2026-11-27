package org.ieeehongik.backend.member.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import org.ieeehongik.backend.member.domain.ParticipationScope;
import org.ieeehongik.backend.member.repository.ParticipationRestrictionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParticipationRestrictionServiceTest {

  @Mock private ParticipationRestrictionRepository participationRestrictionRepository;

  @InjectMocks private ParticipationRestrictionService participationRestrictionService;

  private static final Long ACCOUNT_ID = 10L;

  @Test
  @DisplayName("S-01: 유효한 참여 제한이 없으면 검사를 통과한다")
  void noActiveRestrictionPasses() {
    // given
    when(participationRestrictionRepository.existsActiveRestriction(
            eq(ACCOUNT_ID), eq(ParticipationScope.PROJECT), any(LocalDateTime.class)))
        .thenReturn(false);

    // when & then
    assertDoesNotThrow(
        () ->
            participationRestrictionService.requireParticipationAllowed(
                ACCOUNT_ID, ParticipationScope.PROJECT));
  }

  @Test
  @DisplayName("S-02: 유효한 참여 제한이 존재하면 예외가 발생한다")
  void activeRestrictionThrowsException() {
    // given
    when(participationRestrictionRepository.existsActiveRestriction(
            eq(ACCOUNT_ID), eq(ParticipationScope.STUDY), any(LocalDateTime.class)))
        .thenReturn(true);

    // when
    // TODO(#6): 공통 예외 처리 병합 후 BusinessException 검증으로 변경
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                participationRestrictionService.requireParticipationAllowed(
                    ACCOUNT_ID, ParticipationScope.STUDY));

    // then
    assertEquals("해당 활동에 참여 제한이 적용 중입니다.", exception.getMessage());
  }

  @Test
  @DisplayName("S-03: 계정 ID, 활동 범위, 현재 시각을 Repository에 전달한다")
  void passesCorrectArgumentsToRepository() {
    // given
    Long accountId = 20L;
    ParticipationScope scope = ParticipationScope.STUDY;

    LocalDateTime before = LocalDateTime.now();

    // when
    participationRestrictionService.requireParticipationAllowed(accountId, scope);

    LocalDateTime after = LocalDateTime.now();

    // then
    ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

    verify(participationRestrictionRepository)
        .existsActiveRestriction(eq(accountId), eq(scope), timeCaptor.capture());

    LocalDateTime capturedTime = timeCaptor.getValue();

    assertFalse(capturedTime.isBefore(before));
    assertFalse(capturedTime.isAfter(after));
  }
}
