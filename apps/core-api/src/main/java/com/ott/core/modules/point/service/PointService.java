package com.ott.core.modules.point.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.PointTransaction;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.enums.PointPolicy;
import com.ott.core.modules.point.PointKeyGenerator;
import com.ott.core.modules.point.repository.PointTransactionRepository;
import com.ott.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointService {
    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void rewardAdPoint(Long userId, VideoMetadata video) {
        // 1. 오늘 이미 적립한 횟수 조회
        int currentCount = pointTransactionRepository.countByUserIdAndTypeAndCreatedAtAfter(
                userId, PointTransaction.TransactionType.AD_REWARD, OffsetDateTime.now()
        );

        if (currentCount >= PointPolicy.DAILY_AD_LIMIT.getValue()) {
            throw new BusinessException(ErrorCode.DAILY_LIMIT_OVER);
        }

        // 2. 고유 키 생성 (비즈니스 파라미터 조합)
        String txKey = PointKeyGenerator.generateAdRewardKey(userId, video.getId(), currentCount + 1);

        try {
            // 3. 트랜잭션 로그 생성 시도
            PointTransaction transaction = PointTransaction.builder()
                    .userId(userId)
                    .transactionKey(txKey)
                    .amount(PointPolicy.AD_REWARD.getValue())
                    .type(PointTransaction.TransactionType.AD_REWARD)
                    .referenceId(video.getId())
                    .build();

            pointTransactionRepository.save(transaction); // 여기서 중복이면 Exception 발생

            // 4. 사용자 실제 잔액 업데이트
            pointTransactionRepository.updateUserPoint(userId, PointPolicy.AD_REWARD.getValue());

        } catch (DataIntegrityViolationException e) {
            // DB 레벨에서 중복 키 충돌 시 발생
            log.warn("중복 적립 요청 감지 및 차단: {}", txKey);
            throw new BusinessException(ErrorCode.DUPLICATE_AD_REWARD);
        }
    }
}
