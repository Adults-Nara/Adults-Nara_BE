package com.ott.core.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오 토큰 발급 API 응답
 * POST https://kauth.kakao.com/oauth/token
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoTokenResponse(

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("expires_in")
        Integer expiresIn,

        @JsonProperty("refresh_token_expires_in")
        Integer refreshTokenExpiresIn,

        @JsonProperty("scope")
        String scope
) {
}