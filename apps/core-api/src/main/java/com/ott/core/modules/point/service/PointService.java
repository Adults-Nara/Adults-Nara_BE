package com.ott.core.modules.point.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.PointTransaction;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.enums.PointPolicy;
import com.ott.core.modules.point.PointKeyGenerator;
import com.ott.core.modules.point.dto.PointTransactionHistoryDTO;
import com.ott.core.modules.point.dto.ProductPurchaseRequest;
import com.ott.core.modules.point.repository.PointRepository;
import com.ott.core.modules.point.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointService {
    private final PointTransactionRepository pointTransactionRepository;
    private final PointRepository pointRepository;

    // 광고 시청 시 포인트 지급
    @Transactional
    public void rewardAdPoint(Long userId, VideoMetadata video) {
        // 1. 오늘 이미 적립한 횟수 조회
        OffsetDateTime startOfToday = LocalDate.now(ZoneId.of("Asia/Seoul"))
                .atStartOfDay(ZoneId.of("Asia/Seoul"))
                .toOffsetDateTime();

        int currentCount = pointTransactionRepository.countByUserIdAndTypeAndCreatedAtAfter(
                userId, PointTransaction.TransactionType.AD_REWARD, startOfToday);

        if (currentCount >= PointPolicy.DAILY_AD_LIMIT.getValue()) {
            throw new BusinessException(ErrorCode.DAILY_LIMIT_OVER);
        }

        // 2. 잔액 조회 및 수정
        int currentBalance = pointRepository.findUserPointBalanceByUserId(userId);
        int newBalance = currentBalance + PointPolicy.AD_REWARD.getValue();

        // 3. 고유 키 생성 (비즈니스 파라미터 조합)
        String txKey = PointKeyGenerator.generateAdRewardKey(userId, video.getId(), currentCount + 1);

        try {
            // 4. 트랜잭션 로그 생성 시도
            PointTransaction transaction = PointTransaction.builder()
                    .userId(userId)
                    .transactionKey(txKey)
                    .amount(PointPolicy.AD_REWARD.getValue())
                    .type(PointTransaction.TransactionType.AD_REWARD)
                    .referenceId(video.getId())
                    .balanceAfterTransaction(newBalance)
                    .build();

            pointTransactionRepository.save(transaction);

            // 5. 사용자 실제 잔액 업데이트
            pointTransactionRepository.updateUserPoint(userId, newBalance);

        } catch (DataIntegrityViolationException e) { // DB 레벨에서 중복 키 충돌 시 발생
            log.warn("중복 광고 적립 요청 감지 및 차단: {}", txKey);
            throw new BusinessException(ErrorCode.DUPLICATE_AD_REWARD);
        }
    }

    // 상품 구매 시 포인트 지급
    @Transactional
    public void rewardPurchaseReward(Long userId, ProductPurchaseRequest req) {
        int currentBalance = pointRepository.findUserPointBalanceByUserId(userId);

        int rewardAmount = Math.toIntExact((req.getPrice() * PointPolicy.PURCHASE_RATE.getValue()) / 100);
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
            pointTransactionRepository.updateUserPoint(userId, newBalance);
        } catch (DataIntegrityViolationException e) {
            log.warn("중복 구매 적립 요청 감지 및 차단: {}", txKey);
            throw new BusinessException(ErrorCode.DUPLICATE_PURCHASE_REWARD);
        }
    }

    @Transactional(readOnly = true)
    public int findUserCurrentPoint(Long userId) {
        return pointRepository.findUserPointBalanceByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<PointTransactionHistoryDTO> findUserPointHistory(Long userId, OffsetDateTime startOfDay) {
        List<PointTransaction> transactions = pointTransactionRepository
                .findAllByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, startOfDay);
        return transactions.stream()
                .map(PointTransactionHistoryDTO::from)
                .toList();
    }
}
