package com.ott.core.modules.uplus.util;

/**
 * 전화번호 관련 유틸리티
 */
public class PhoneNumberUtils {

    private PhoneNumberUtils() {}

    /**
     * 전화번호에서 숫자만 추출 (하이픈 제거)
     * ex) "010-1111-1111" → "01011111111"
     */
    public static String normalize(String phone) {
        return phone == null ? null : phone.replaceAll("[^0-9]", "");
    }

    /**
     * 전화번호 마스킹 (저장된 숫자만 있는 형태 기준)
     * ex) "01011111111" → "010-****-1111"
     */
    public static String mask(String phone) {
        if (phone == null || phone.length() < 11) return phone;
        return phone.substring(0, 3) + "-****-" + phone.substring(7);
    }
}