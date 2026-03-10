package com.ott.core.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * 쿠키 관련 공통 유틸리티
 *
 * AuthController, BackofficeAuthController 등에서 공통으로 사용합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CookieUtils {

    /**
     * 요청 쿠키에서 특정 이름의 쿠키 값을 추출합니다.
     *
     * @param request    HttpServletRequest
     * @param cookieName 추출할 쿠키 이름
     * @return 쿠키 값 (없거나 비어있으면 null)
     */
    public static String extractValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return null;
        }
        String value = Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        // 빈 문자열도 null로 처리 (명시적 실패 처리를 호출부에서 일관되게 수행)
        return StringUtils.hasText(value) ? value : null;
    }
}