package com.ott.core.modules.point.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.PointTransaction;
import com.ott.common.persistence.entity.UserPointBalance;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.enums.PointPolicy;
import com.ott.core.modules.point.PointKeyGenerator;
import com.ott.core.modules.point.dto.PointTransactionHistoryRequest;
import com.ott.core.modules.point.dto.PointTransactionHistoryResponse;
import com.ott.core.modules.point.dto.ProductPurchaseRequest;
import com.ott.core.modules.point.dto.UserPointBalanceResponse;
import com.ott.core.modules.point.repository.PointRepository;
import com.ott.core.modules.point.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointService {
    private final PointTransactionRepository pointTransactionRepository;
    private final PointRepository pointRepository;
    private final PointPolicyService pointPolicyService;

    // 광고 시청 시 포인트 지급
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rewardAdPoint(Long userId, VideoMetadata videoMetadata) {
        // 1. 오늘 이미 적립한 횟수 조회
        OffsetDateTime startOfToday = LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay(ZoneOffset.UTC)
                .toOffsetDateTime();

        int currentCount = pointTransactionRepository.countByUserIdAndTypeAndCreatedAtAfter(
                userId, PointTransaction.TransactionType.AD_REWARD, startOfToday);

        int dailyLimit = pointPolicyService.getPolicyValue(PointPolicy.DAILY_AD_LIMIT);
        if (currentCount >= dailyLimit) {
            throw new BusinessException(ErrorCode.DAILY_LIMIT_OVER);
        }

        // 2. 잔액 조회 및 수정
        int currentBalance = pointRepository.findUserPointBalanceByUserId(userId).getCurrentBalance();
        int adRewardValue = pointPolicyService.getPolicyValue(PointPolicy.AD_REWARD);
        int newBalance = currentBalance + adRewardValue;

        // 3. 고유 키 생성 (비즈니스 파라미터 조합)
        String txKey = PointKeyGenerator.generateAdRewardKey(userId, videoMetadata.getId(), currentCount + 1);

        try {
            // 4. 트랜잭션 로그 생성 시도
            PointTransaction transaction = PointTransaction.builder()
                    .userId(userId)
                    .transactionKey(txKey)
                    .amount(adRewardValue)
                    .type(PointTransaction.TransactionType.AD_REWARD)
                    .referenceId(videoMetadata.getId())
                    .balanceAfterTransaction(newBalance)
                    .build();

            pointTransactionRepository.save(transaction);

            // 5. 사용자 실제 잔액 업데이트
            OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);
            pointTransactionRepository.updateUserPoint(userId, newBalance, nowUtc);

        } catch (DataIntegrityViolationException e) { // DB 레벨에서 중복 키 충돌 시 발생
            log.warn("중복 광고 적립 요청 감지 및 차단: {}", txKey);
            throw new BusinessException(ErrorCode.DUPLICATE_AD_REWARD);
        }
    }

    // 상품 구매 시 포인트 지급
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rewardPurchaseBonus(Long userId, ProductPurchaseRequest req) {
        int currentBalance = pointRepository.findUserPointBalanceByUserId(userId).getCurrentBalance();

        int purchaseRate = pointPolicyService.getPolicyValue(PointPolicy.PURCHASE_RATE);
        int rewardAmount = Math.toIntExact((req.getPrice() * purchaseRate) / 100);
        int newBalance = currentBalance + rewardAmount;

        String txKey = PointKeyGenerator.generatePurchaseKey(userId, req.getOrderId());

        try {
            PointTransaction transaction = PointTransaction.builder()
                    .userId(userId)
                    .transactionKey(txKey)
                    .amount(rewardAmount)
                    .type(PointTransaction.TransactionType.PURCHASE_BONUS)
                    .referenceId(req.getOrderId())
                    .balanceAfterTransaction(newBalance)
                    .build();
            pointTransactionRepository.save(transaction);

            OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);
            pointTransactionRepository.updateUserPoint(userId, newBalance, nowUtc);
        } catch (DataIntegrityViolationException e) {
            log.warn("중복 구매 적립 요청 감지 및 차단: {}", txKey);
            throw new BusinessException(ErrorCode.DUPLICATE_PURCHASE_REWARD);
        }
    }

    @Transactional(readOnly = true)
    public UserPointBalanceResponse findUserCurrentPoint(Long userId) {
        UserPointBalance userPointBalance = pointRepository.findUserPointBalanceByUserId(userId);
        return UserPointBalanceResponse.builder()
                .currentBalance(userPointBalance.getCurrentBalance())
                .lastUpdated(userPointBalance.getLastUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PointTransactionHistoryResponse> findUserPointHistory(Long userId, PointTransactionHistoryRequest req) {

        OffsetDateTime start = LocalDate.parse(req.getStartDate())
                .atStartOfDay(ZoneOffset.UTC)
                .toOffsetDateTime();

        OffsetDateTime end = LocalDate.parse(req.getEndDate())
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toOffsetDateTime()
                .minusNanos(1);

        List<PointTransaction> transactions = pointTransactionRepository
                .findAllByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, start, end);
        return transactions.stream()
                .map(PointTransactionHistoryResponse::from)
                .toList();
    }
}
