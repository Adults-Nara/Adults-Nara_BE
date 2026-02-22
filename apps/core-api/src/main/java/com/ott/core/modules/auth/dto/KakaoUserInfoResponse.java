package com.ott.core.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오 사용자 정보 조회 API 응답
 * GET https://kapi.kakao.com/v2/user/me
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(

        @JsonProperty("id")
        Long id,

        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(

            @JsonProperty("email")
            String email,

            @JsonProperty("is_email_valid")
            Boolean isEmailValid,

            @JsonProperty("is_email_verified")
            Boolean isEmailVerified,

            @JsonProperty("profile")
            Profile profile
    ) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Profile(

                @JsonProperty("nickname")
                String nickname,

                @JsonProperty("profile_image_url")
                String profileImageUrl,

                @JsonProperty("thumbnail_image_url")
                String thumbnailImageUrl,

                @JsonProperty("is_default_image")
                Boolean isDefaultImage
        ) {
        }
    }

    // ====== 편의 메서드 ======

    /**
     * 카카오 고유 ID (문자열)
     */
    public String oauthId() {
        return id.toString();
    }

    /**
     * 이메일 (없으면 "kakao_{id}@kakao.local" 형태로 생성)
     */
    public String email() {
        if (kakaoAccount != null && kakaoAccount.email() != null) {
            return kakaoAccount.email();
        }
        return "kakao_" + id + "@kakao.local";
    }

    /**
     * 닉네임 (없으면 "카카오유저_{id}" 형태로 생성)
     */
    public String nickname() {
        if (kakaoAccount != null && kakaoAccount.profile() != null
                && kakaoAccount.profile().nickname() != null) {
            return kakaoAccount.profile().nickname();
        }
        return "카카오유저_" + id;
    }

    /**
     * 프로필 이미지 URL (없으면 null)
     */
    public String profileImageUrl() {
        if (kakaoAccount != null && kakaoAccount.profile() != null) {
            return kakaoAccount.profile().profileImageUrl();
        }
        return null;
    }
}