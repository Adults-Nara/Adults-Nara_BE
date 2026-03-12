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
     * 전화번호로 U+ 가입 정보 확인
     * 1. 전화번호 + userId DB 조회 → 미존재 시 UPLUS_PHONE_NOT_FOUND
     * 2. active 여부 확인 → 해지 상태면 UPLUS_SUBSCRIPTION_INACTIVE
     * 3. 성공 → 가입 정보 확인 응답
     */
    @Transactional(readOnly = true)
    public UPlusSubscriptionDto.LinkResponse verify(Long userId, UPlusSubscriptionDto.LinkRequest request) {
        String phoneNumber = normalize(request.getPhoneNumber());

        UPlusSubscription subscription = subscriptionRepository.findByPhoneNumberAndUserId(phoneNumber, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UPLUS_PHONE_NOT_FOUND));

        ensureSubscriptionIsActive(subscription);

        return UPlusSubscriptionDto.LinkResponse.success(subscription);
    }

    /**
     * 내 U+ 가입 정보 조회
     */
    @Transactional(readOnly = true)
    public UPlusSubscriptionDto.SubscriptionResponse getMySubscription(Long userId) {
        UPlusSubscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UPLUS_NOT_REGISTERED));

        ensureSubscriptionIsActive(subscription);

        return UPlusSubscriptionDto.SubscriptionResponse.from(subscription);
    }

    // ====== Private Methods ======

    private void ensureSubscriptionIsActive(UPlusSubscription subscription) {
        if (!subscription.isActive()) {
            throw new BusinessException(ErrorCode.UPLUS_SUBSCRIPTION_INACTIVE);
        }
    }

    private String normalize(String phone) {
        return phone == null ? null : phone.replaceAll("[^0-9]", "");
    }
}