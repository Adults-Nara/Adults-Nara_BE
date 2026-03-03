package com.ott.core.modules.search.util;

public class ChosungUtils {
    private static final char[] CHOSUNG_LIST = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

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
            if (ch >= 0xAC00 && ch <= 0xD7A3) {
                int index = (ch - 0xAC00) / (21 * 28);
                result.append(CHOSUNG_LIST[index]);
            } else {
                result.append(ch); // 한글이 아니면 그대로 통과
            }
        }
        return result.toString();
    }
}