package com.ott.core.modules.search.util;

public final class ChosungUtils {
    private static final char[] CHOSUNG_LIST = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    // 매직 넘버 상수화
    private static final int JUNGSEONG_COUNT = 21;
    private static final int JONGSEONG_COUNT = 28;
    private static final int CHOSUNG_BASE = JUNGSEONG_COUNT * JONGSEONG_COUNT; // 588

    private ChosungUtils() {
        throw new UnsupportedOperationException("이 클래스는 유틸리티 클래스이므로 인스턴스화할 수 없습니다.");
    }
    /**
     * 문자열을 받아서 초성만 추출합니다.
     */
    public static String extract(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            // 한글(가~힣) 범위 안에 있는지 확인
            if (ch >= '가' && ch <= '힣') {
                int index = (ch - '가') / CHOSUNG_BASE;
                result.append(CHOSUNG_LIST[index]);
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}