package com.ott.core.modules.auth.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;

import com.ott.common.persistence.entity.User;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.usertag.repository.UserTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository userRepository;
    private final UserTagRepository userTagRepository;

    /**
     * 온보딩 완료 처리.
     * 태그를 1개 이상 선택한 경우에만 완료 처리한다.
     *
     * @param userId 사용자 ID
     */
    @Transactional
    public void complete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isOnboardingCompleted()) {
            log.info("[온보딩] 이미 완료된 사용자 - userId: {}", userId);
            return;
        }

        boolean hasTag = userTagRepository.existsByUserId(userId);
        if (!hasTag) {
            log.warn("[온보딩] 태그 미선택 상태로 완료 시도 - userId: {}", userId);
            throw new BusinessException(ErrorCode.INVALID_ONBOARDING_STATE);
        }

        user.completeOnboarding();
        log.info("[온보딩] 온보딩 완료 처리 - userId: {}", userId);
    }
}