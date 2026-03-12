package com.ott.core.modules.uplus.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.UPlusSubscription;
import com.ott.core.modules.uplus.dto.UPlusSubscriptionDto;
import com.ott.core.modules.uplus.repository.UPlusSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UPlusSubscriptionService {

    private final UPlusSubscriptionRepository subscriptionRepository;

    /**
     * 전화번호로 U+ 연동
     * 1. 전화번호로 uplus_subscription 조회 → 없으면 "가입되지 않은 번호"
     * 2. userId 일치 여부 확인 → 불일치 시 "가입 정보를 찾을 수 없습니다"
     * 3. active 여부 확인 → 해지 상태면 에러
     * 4. 성공 → 연동 완료 응답
     */
    @Transactional(readOnly = true)
    public UPlusSubscriptionDto.LinkResponse link(Long userId, UPlusSubscriptionDto.LinkRequest request) {
        String phoneNumber = normalize(request.getPhoneNumber());

        UPlusSubscription subscription = subscriptionRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.UPLUS_PHONE_NOT_FOUND));

        if (!subscription.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UPLUS_PHONE_USER_MISMATCH);
        }

        if (!subscription.isActive()) {
            throw new BusinessException(ErrorCode.UPLUS_SUBSCRIPTION_INACTIVE);
        }

        return UPlusSubscriptionDto.LinkResponse.success(subscription);
    }

    /**
     * 내 U+ 가입 정보 조회
     */
    @Transactional(readOnly = true)
    public UPlusSubscriptionDto.SubscriptionResponse getMySubscription(Long userId) {
        UPlusSubscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UPLUS_NOT_REGISTERED));

        if (!subscription.isActive()) {
            throw new BusinessException(ErrorCode.UPLUS_SUBSCRIPTION_INACTIVE);
        }

        return UPlusSubscriptionDto.SubscriptionResponse.from(subscription);
    }

    // ===== Private =====

    private String normalize(String phone) {
        return phone == null ? null : phone.replaceAll("[^0-9]", "");
    }
}