package com.ott.core.modules.uplus.service;

import com.ott.common.persistence.entity.PointTransaction;
import com.ott.common.persistence.entity.UPlusBillDiscount;
import com.ott.common.persistence.entity.UPlusSubscription;
import com.ott.core.modules.point.repository.PointRepository;
import com.ott.core.modules.point.repository.PointTransactionRepository;
import com.ott.core.modules.uplus.repository.UPlusBillDiscountRepository;
import com.ott.core.modules.uplus.repository.UPlusSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * U+ 할인 처리 실행기
 *
 * UPlusDiscountScheduler에서 self-invocation 문제를 피하기 위해 분리.
 * @Transactional이 Spring 프록시를 통해 정상 동작하도록 별도 빈으로 관리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UPlusDiscountProcessor {

    private final UPlusSubscriptionRepository subscriptionRepository;
    private final UPlusBillDiscountRepository billDiscountRepository;
    private final PointRepository pointRepository;
    private final PointTransactionRepository pointTransactionRepository;

    /**
     * 유저 1명에 대한 할인 처리 (트랜잭션 적용)
     * 한 명 실패가 다른 유저에게 영향 없도록 스케줄러에서 개별 try-catch로 감쌈
     *
     * @return true = 할인 처리됨, false = 포인트 0이라 스킵
     */
    @Transactional
    public boolean processUserDiscount(Long userId, String billingYearMonth) {
        // 1. 중복 실행 방지
        if (billDiscountRepository.existsByUserIdAndBillingYearMonth(userId, billingYearMonth)) {
            log.warn("[UPlus 할인] 유저 {} 는 {} 에 이미 처리됨, 건너뜀", userId, billingYearMonth);
            return false;
        }

        // 2. 가입 정보 조회 — findActiveUserIds와 이 메서드 사이 경쟁 조건으로 없을 수 있음
        Optional<UPlusSubscription> subscriptionOpt = subscriptionRepository.findByUserId(userId);
        if (subscriptionOpt.isEmpty()) {
            log.warn("[UPlus 할인] 유저 {} 가입 정보 없음 (조회 후 삭제된 경우), 건너뜀", userId);
            return false;
        }
        UPlusSubscription subscription = subscriptionOpt.get();

        // 3. 포인트 잔액 조회 (비관적 락 — 동시성 보호)
        // 포인트를 한 번도 적립한 적 없는 유저는 null 반환 가능
        var balance = pointRepository.findUserPointBalanceByUserIdUpdateLock(userId);
        int currentPoints = (balance != null) ? balance.getCurrentBalance() : 0;

        if (currentPoints <= 0) {
            // 포인트 없음 → 0원 이력만 기록
            billDiscountRepository.save(UPlusBillDiscount.builder()
                    .userId(userId)
                    .billingYearMonth(billingYearMonth)
                    .plan(subscription.getPlan())
                    .discountAmount(0)
                    .build());
            log.debug("[UPlus 할인] 유저 {} 포인트 없음, 0원 이력 기록", userId);
            return false;
        }

        // 4. 할인 금액 산출: min(보유 포인트, 요금제 월 기본료)
        int discountAmount = Math.min(currentPoints, subscription.getPlan().getMonthlyFee());
        int newBalance = currentPoints - discountAmount;

        // 5. 포인트 차감 트랜잭션 기록
        PointTransaction transaction = PointTransaction.builder()
                .userId(userId)
                .transactionKey("UPLUS_DISCOUNT_" + billingYearMonth + "_" + userId)
                .amount(-discountAmount)
                .type(PointTransaction.TransactionType.UPLUS_DISCOUNT)
                .referenceId(null)
                .balanceAfterTransaction(newBalance)
                .build();
        pointTransactionRepository.save(transaction);

        // 6. 잔액 업데이트
        balance.setCurrentBalance(newBalance);
        balance.setLastUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        // 7. 할인 이력 기록
        billDiscountRepository.save(UPlusBillDiscount.builder()
                .userId(userId)
                .billingYearMonth(billingYearMonth)
                .plan(subscription.getPlan())
                .discountAmount(discountAmount)
                .build());

        log.debug("[UPlus 할인] 유저 {} 처리 완료 - 할인: {}원, 잔액: {}점, 청구월: {}",
                userId, discountAmount, newBalance, billingYearMonth);

        return true;
    }
}