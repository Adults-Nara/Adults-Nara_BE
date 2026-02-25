package com.ott.core.modules.point;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class PointKeyGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 광고 시청 적립용 고유 키 생성
     * 
     * @param userId        사용자 ID
     * @param videoId       비디오 ID
     * @param dailySequence 오늘 몇 번째 적립인지 (1~10)
     */
    public static String generateAdRewardKey(Long userId, Long videoId, int dailySequence) {
        return String.format("AD_%d_%d_%s_%d",
                userId,
                videoId,
                LocalDate.now(ZoneId.of("Asia/Seoul")).format(DATE_FORMATTER),
                dailySequence);
    }

    // 상품 구매 적립용 고유 키 생성
    public static String generatePurchaseKey(Long userId, Long orderId) {
        return String.format("BUY_%d_%d", userId, orderId);
    }
}