package com.ott.core.modules.point.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.PointTransaction;
import com.ott.common.persistence.entity.UserPointBalance;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.enums.PointPolicy;
import com.ott.core.modules.point.dto.PointTransactionHistoryRequest;
import com.ott.core.modules.point.dto.PointTransactionHistoryResponse;
import com.ott.core.modules.point.dto.ProductPurchaseRequest;
import com.ott.core.modules.point.repository.PointRepository;
import com.ott.core.modules.point.repository.PointTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private PointRepository pointRepository;

    @Test
    @DisplayName("광고 시청 보상 적립 성공")
    void rewardAdPoint_success() {
        // given
        Long userId = 1L;
        VideoMetadata video = VideoMetadata.builder().id(100L).build();
        int currentBalance = 1000;

        UserPointBalance userPointBalance = mock(UserPointBalance.class);
        given(userPointBalance.getCurrentBalance()).willReturn(currentBalance);

        given(pointTransactionRepository.countByUserIdAndTypeAndCreatedAtAfter(eq(userId),
                eq(PointTransaction.TransactionType.AD_REWARD), any(OffsetDateTime.class)))
                .willReturn(0);
        given(pointRepository.findUserPointBalanceByUserId(userId)).willReturn(userPointBalance);

        // when
        pointService.rewardAdPoint(userId, video);

        // then
        int expectedNewBalance = currentBalance + PointPolicy.AD_REWARD.getValue();
        verify(pointTransactionRepository).save(any(PointTransaction.class));
        verify(pointTransactionRepository).updateUserPoint(userId, expectedNewBalance);
    }

    @Test
    @DisplayName("일일 광고 시청 제한 초과 시 예외 발생")
    void rewardAdPoint_dailyLimitExceeded() {
        // given
        Long userId = 1L;
        VideoMetadata video = VideoMetadata.builder().id(100L).build();

        given(pointTransactionRepository.countByUserIdAndTypeAndCreatedAtAfter(eq(userId),
                eq(PointTransaction.TransactionType.AD_REWARD), any(OffsetDateTime.class)))
                .willReturn(PointPolicy.DAILY_AD_LIMIT.getValue());

        // when & then
        assertThatThrownBy(() -> pointService.rewardAdPoint(userId, video))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DAILY_LIMIT_OVER);
    }

    @Test
    @DisplayName("중복 광고 시청 보상 시 예외 발생 (DB 키 제약 조건)")
    void rewardAdPoint_duplicateKey() {
        // given
        Long userId = 1L;
        VideoMetadata video = VideoMetadata.builder().id(100L).build();
        int currentBalance = 1000;

        UserPointBalance userPointBalance = mock(UserPointBalance.class);
        given(userPointBalance.getCurrentBalance()).willReturn(currentBalance);

        given(pointTransactionRepository.countByUserIdAndTypeAndCreatedAtAfter(eq(userId),
                eq(PointTransaction.TransactionType.AD_REWARD), any(OffsetDateTime.class)))
                .willReturn(0);
        given(pointRepository.findUserPointBalanceByUserId(userId)).willReturn(userPointBalance);
        given(pointTransactionRepository.save(any(PointTransaction.class)))
                .willThrow(new DataIntegrityViolationException("Unique index violation"));

        // when & then
        assertThatThrownBy(() -> pointService.rewardAdPoint(userId, video))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_AD_REWARD);
    }

    @Test
    @DisplayName("상품 구매 보상 적립 성공")
    void rewardPurchaseReward_success() {
        // given
        Long userId = 1L;
        ProductPurchaseRequest request = new ProductPurchaseRequest(200L, 101L, 10000L); // orderId, productId, price
        int currentBalance = 1000;

        UserPointBalance userPointBalance = mock(UserPointBalance.class);
        given(userPointBalance.getCurrentBalance()).willReturn(currentBalance);

        given(pointRepository.findUserPointBalanceByUserId(userId)).willReturn(userPointBalance);

        // when
        pointService.rewardPurchaseReward(userId, request);

        // then
        int expectedRewardAmount = Math.toIntExact((request.getPrice() * PointPolicy.PURCHASE_RATE.getValue()) / 100);
        int expectedNewBalance = currentBalance + expectedRewardAmount;

        verify(pointTransactionRepository).save(any(PointTransaction.class));
        verify(pointTransactionRepository).updateUserPoint(userId, expectedNewBalance);
    }

    @Test
    @DisplayName("사용자 현재 잔액 조회")
    void findUserCurrentPoint() {
        // given
        Long userId = 1L;
        int expectedBalance = 5000;
        UserPointBalance userPointBalance = mock(UserPointBalance.class);
        given(userPointBalance.getCurrentBalance()).willReturn(expectedBalance);
        given(pointRepository.findUserPointBalanceByUserId(userId)).willReturn(userPointBalance);

        // when
        int actualBalance = pointService.findUserCurrentPoint(userId).getCurrentBalance();

        // then
        assertThat(actualBalance).isEqualTo(expectedBalance);
    }

    @Test
    @DisplayName("사용자 포인트 내역 조회")
    void findUserPointHistory() {
        // given
        Long userId = 1L;
        OffsetDateTime startOfDay = OffsetDateTime.now();
        OffsetDateTime endOfDay = OffsetDateTime.now().plusDays(1);
        PointTransactionHistoryRequest request = new PointTransactionHistoryRequest(startOfDay, endOfDay);

        PointTransaction tx1 = PointTransaction.builder()
                .id(1L)
                .amount(5)
                .type(PointTransaction.TransactionType.AD_REWARD)
                .build();
        PointTransaction tx2 = PointTransaction.builder()
                .id(2L)
                .amount(100)
                .type(PointTransaction.TransactionType.PURCHASE_BONUS)
                .build();

        given(pointTransactionRepository.findAllByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, startOfDay,
                endOfDay))
                .willReturn(List.of(tx1, tx2));

        // when
        List<PointTransactionHistoryResponse> history = pointService.findUserPointHistory(userId, request);

        // then
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getTransactionId()).isEqualTo(1L);
        assertThat(history.get(1).getTransactionId()).isEqualTo(2L);
    }
}
