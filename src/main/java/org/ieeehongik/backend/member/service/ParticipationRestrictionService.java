package org.ieeehongik.backend.member.service;

import org.ieeehongik.backend.member.domain.ParticipationScope;
import org.ieeehongik.backend.member.repository.ParticipationRestrictionRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRestrictionService {

    private final ParticipationRestrictionRepository participationRestrictionRepository;

    public void requireParticipationAllowed(
            Long accountId,
            ParticipationScope scope
    ) {
        boolean isRestricted = participationRestrictionRepository
                .existsActiveRestriction(
                        accountId,
                        scope,
                        LocalDateTime.now()
                );

        if (isRestricted) {
            // TODO(#6): 공통 예외 처리 PR 병합 후
            // BusinessException(MemberErrorCode.PARTICIPATION_RESTRICTED)으로 교체
            throw new IllegalStateException("해당 활동에 참여 제한이 적용 중입니다.");
        }
    }
}
