package com.ott.core.modules.uplus.client;

import com.ott.common.persistence.enums.UPlusSubscriptionPlan;

import java.util.Optional;

/**
 * U+ 가입 정보 조회 인터페이스
 *
 * 현재: MockUPlusApiClient
 * 추후: 실제 U+ API 연동 시 구현체만 교체, 이 인터페이스는 변경 없음
 */
public interface UPlusApiClient {

    /**
     * 전화번호로 U+ 가입 정보를 조회한다.
     *
     * @param phoneNumber 하이픈 없는 숫자만 (ex. "01012345678")
     * @return 가입자 → 요금제가 담긴 Optional / 비가입자 → Optional.empty()
     */
    Optional<UPlusSubscriptionPlan> findSubscriberPlan(String phoneNumber);
}