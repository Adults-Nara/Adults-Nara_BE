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

    @Transactional(readOnly = true)
    public UPlusSubscriptionDto.SubscriptionResponse getMySubscription(Long userId) {
        UPlusSubscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UPLUS_NOT_REGISTERED));
        if (!subscription.isActive()) {
            throw new BusinessException(ErrorCode.UPLUS_SUBSCRIPTION_INACTIVE);
        }
        return UPlusSubscriptionDto.SubscriptionResponse.from(subscription);
    }
}