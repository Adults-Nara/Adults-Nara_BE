package com.ott.core.modules.uplus.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.UPlusSubscription;
import com.ott.common.persistence.enums.UPlusSubscriptionPlan;
import com.ott.core.modules.uplus.client.UPlusApiClient;
import com.ott.core.modules.uplus.dto.UPlusSubscriptionDto;
import com.ott.core.modules.uplus.repository.UPlusSubscriptionRepository;
import com.ott.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UPlusSubscriptionService {

    private final UPlusSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final UPlusApiClient uPlusApiClient;

    // ===== 요금제 목록 =====

    public List<UPlusSubscriptionDto.PlanInfo> getPlans() {
        return Arrays.stream(UPlusSubscriptionPlan.values())
                .map(p -> new UPlusSubscriptionDto.PlanInfo(
                        p.name(), p.getDisplayName(), p.getMonthlyFee()))
                .toList();
    }

    // ===== U+ 가입 정보 등록 =====

    /**
     * 전화번호 입력 → U+ API로 가입 여부 + 요금제 조회 → 자동 등록
     *
     * 비가입자: registered=false 응답 반환 (DB 저장 없음)
     * 가입자:   요금제 자동 세팅 후 DB 저장, registered=true 응답 반환
     */
    @Transactional
    public UPlusSubscriptionDto.RegisterResponse register(
            Long userId, UPlusSubscriptionDto.RegisterRequest request) {

        // 유저 존재 여부 확인
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String phoneNumber = normalizePhone(request.getPhoneNumber());

        // U+ API 조회: 비가입자면 Optional.empty()
        Optional<UPlusSubscriptionPlan> planOpt = uPlusApiClient.findSubscriberPlan(phoneNumber);
        if (planOpt.isEmpty()) {
            return UPlusSubscriptionDto.RegisterResponse.notSubscriber();
        }

        UPlusSubscriptionPlan plan = planOpt.get();

        // 이미 등록된 경우 — existsByUserId 없이 findByUserId 단일 쿼리로 처리
        Optional<UPlusSubscription> existingOpt = subscriptionRepository.findByUserId(userId);
        if (existingOpt.isPresent()) {
            UPlusSubscription existing = existingOpt.get();
            if (existing.isActive()) {
                throw new BusinessException(ErrorCode.UPLUS_ALREADY_REGISTERED);
            }
            // 해지 상태면 재가입 — 전화번호 + 요금제 모두 최신 값으로 갱신
            existing.reactivate(phoneNumber, plan);
            return UPlusSubscriptionDto.RegisterResponse.success(existing);
        }

        // 신규 등록 — @Builder 패턴 사용
        UPlusSubscription subscription = UPlusSubscription.builder()
                .userId(userId)
                .phoneNumber(phoneNumber)
                .plan(plan)
                .active(true)
                .build();
        subscriptionRepository.save(subscription);
        return UPlusSubscriptionDto.RegisterResponse.success(subscription);
    }

    // ===== 내 가입 정보 조회 =====

    @Transactional(readOnly = true)
    public UPlusSubscriptionDto.SubscriptionResponse getMySubscription(Long userId) {
        UPlusSubscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UPLUS_NOT_REGISTERED));
        if (!subscription.isActive()) {
            throw new BusinessException(ErrorCode.UPLUS_SUBSCRIPTION_INACTIVE);
        }
        return UPlusSubscriptionDto.SubscriptionResponse.from(subscription);
    }

    // ===== 요금제 동기화 =====

    /**
     * U+ API를 재조회해서 최신 요금제로 동기화
     */
    @Transactional
    public UPlusSubscriptionDto.SubscriptionResponse syncPlan(Long userId) {
        UPlusSubscription subscription = getActiveSubscription(userId);

        Optional<UPlusSubscriptionPlan> planOpt =
                uPlusApiClient.findSubscriberPlan(subscription.getPhoneNumber());

        if (planOpt.isEmpty()) {
            subscription.deactivate();
            throw new BusinessException(ErrorCode.UPLUS_SUBSCRIPTION_INACTIVE);
        }

        subscription.changePlan(planOpt.get());
        return UPlusSubscriptionDto.SubscriptionResponse.from(subscription);
    }

    // ===== 해지 =====

    @Transactional
    public void deactivate(Long userId) {
        UPlusSubscription subscription = getActiveSubscription(userId);
        subscription.deactivate();
    }

    // ===== 헬퍼 =====

    private UPlusSubscription getActiveSubscription(Long userId) {
        UPlusSubscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UPLUS_NOT_REGISTERED));
        if (!subscription.isActive()) {
            throw new BusinessException(ErrorCode.UPLUS_SUBSCRIPTION_INACTIVE);
        }
        return subscription;
    }

    private static String normalizePhone(String phone) {
        return phone == null ? null : phone.replaceAll("[^0-9]", "");
    }
}