package com.ott.core.modules.uplus.client;

import com.ott.common.persistence.enums.UPlusSubscriptionPlan;
import com.ott.core.modules.uplus.util.PhoneNumberUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * U+ API Mock 구현체
 *
 * 실제 U+ API 연동 전까지 사용.
 * 추후 실제 구현체 작성 후 이 클래스는 제거 또는 test profile로 이동.
 *
 * 테스트 데이터:
 *   01011111111 → 5G 프리미엄 가입자
 *   01022222222 → LTE 스탠다드 가입자
 *   그 외       → 비가입자 (Optional.empty)
 */
@Slf4j
@Component
public class MockUPlusApiClient implements UPlusApiClient {

    private static final Map<String, UPlusSubscriptionPlan> MOCK_DATA = Map.of(
            "01011111111", UPlusSubscriptionPlan.FIVE_G_PREMIUM,
            "01022222222", UPlusSubscriptionPlan.LTE_STANDARD
    );

    @Override
    public Optional<UPlusSubscriptionPlan> findSubscriberPlan(String phoneNumber) {
        log.info("[Mock U+ API] 가입 정보 조회 - phoneNumber: {}", PhoneNumberUtils.mask(phoneNumber));

        Optional<UPlusSubscriptionPlan> result = Optional.ofNullable(MOCK_DATA.get(phoneNumber));
        log.info("[Mock U+ API] 조회 결과: {}",
                result.map(p -> "가입자 (" + p.getDisplayName() + ")").orElse("비가입자"));

        return result;
    }
}