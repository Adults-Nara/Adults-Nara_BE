package com.ott.core.modules.uplus.service;

import com.ott.core.modules.uplus.repository.UPlusSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * U+ 포인트 자동 할인 스케줄러
 *
 * 실행 시점: 매월 1일 00:00 (UTC)
 *
 * 주의: processUserDiscount()는 UPlusDiscountProcessor에 위임.
 * self-invocation 시 @Transactional이 동작하지 않으므로 별도 빈으로 분리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UPlusDiscountScheduler {

    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM");

    private final UPlusSubscriptionRepository subscriptionRepository;
    private final UPlusDiscountProcessor discountProcessor;

    @Scheduled(cron = "0 0 0 1 * *")
    public void applyMonthlyDiscount() {
        String billingYearMonth = LocalDate.now(ZoneOffset.UTC).format(MONTH_FORMATTER);
        log.info("[UPlus 할인 스케줄러] 시작 - 청구월: {}", billingYearMonth);

        List<Long> activeUserIds = subscriptionRepository.findActiveUserIds();
        log.info("[UPlus 할인 스케줄러] 처리 대상 가입자 수: {}", activeUserIds.size());

        int successCount = 0;
        int skipCount = 0;

        for (Long userId : activeUserIds) {
            try {
                boolean processed = discountProcessor.processUserDiscount(userId, billingYearMonth);
                if (processed) successCount++;
                else skipCount++;
            } catch (Exception e) {
                log.error("[UPlus 할인 스케줄러] 유저 {} 처리 실패: {}", userId, e.getMessage(), e);
            }
        }

        log.info("[UPlus 할인 스케줄러] 완료 - 성공: {}, 포인트 없음: {}, 청구월: {}",
                successCount, skipCount, billingYearMonth);
    }
}