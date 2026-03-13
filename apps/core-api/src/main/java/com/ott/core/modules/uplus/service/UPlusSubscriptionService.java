package com.ott.core.modules.uplus.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.UPlusSubscription;
import com.ott.core.modules.uplus.dto.UPlusSubscriptionDto;
import com.ott.core.modules.uplus.repository.UPlusSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UPlusSubscriptionService {

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^01[016789]-?\\d{3,4}-?\\d{4}$");

    private final UPlusSubscriptionRepository subscriptionRepository;

    /**
     * 전화번호로 U+ 가입 정보 확인
     * - 모든 실패 케이스(형식 오류, 미존재, userId 불일치, 해지, 예외)는 verified=false로 반환
     * - 예외를 던지지 않으므로 프론트는 verified 필드로만 분기
     */
    @Transactional(readOnly = true)
    public UPlusSubscriptionDto.LinkResponse verify(Long userId, UPlusSubscriptionDto.LinkRequest request) {
        try {
            String rawPhone = request.getPhoneNumber();

            if (rawPhone == null || rawPhone.isBlank()) {
                return UPlusSubscriptionDto.LinkResponse.notFound();
            }

            if (!PHONE_PATTERN.matcher(rawPhone).matches()) {
                return UPlusSubscriptionDto.LinkResponse.wrongNumber();
            }

            String phoneNumber = normalize(rawPhone);

            Optional<UPlusSubscription> subscriptionOpt =
                    subscriptionRepository.findByPhoneNumberAndUserId(phoneNumber, userId);

            if (subscriptionOpt.isEmpty()) {
                return UPlusSubscriptionDto.LinkResponse.notFound();
            }

            UPlusSubscription subscription = subscriptionOpt.get();

            if (!subscription.isActive()) {
                return UPlusSubscriptionDto.LinkResponse.inactive();
            }

            return UPlusSubscriptionDto.LinkResponse.success(subscription);

        } catch (Exception e) {
            return UPlusSubscriptionDto.LinkResponse.notFound();
        }
    }

    /**
     * 내 U+ 가입 정보 조회
     * 가입 정보가 없는 경우 UPLUS_NOT_REGISTERED 반환
     * active=false(해지) 상태도 그대로 반환
     */
    @Transactional(readOnly = true)
    public UPlusSubscriptionDto.SubscriptionResponse getMySubscription(Long userId) {
        UPlusSubscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UPLUS_NOT_REGISTERED));

        return UPlusSubscriptionDto.SubscriptionResponse.from(subscription);
    }

    // ====== Private Methods ======

    private String normalize(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }
}