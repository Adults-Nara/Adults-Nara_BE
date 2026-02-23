package com.ott.core.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오 사용자 정보 조회 API 응답
 * GET https://kapi.kakao.com/v2/user/me
 *
 * @see <a href="https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#req-user-info">카카오 문서</a>
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
     * [Fix #1] 카카오 이메일 검증 여부 확인
     * 미검증 이메일로 기존 계정에 연동하면 계정 탈취 가능
     *
     * @return true: 이메일이 유효하고 검증됨, false: 그 외
     */
    public boolean isEmailVerified() {
        if (kakaoAccount == null) return false;
        return Boolean.TRUE.equals(kakaoAccount.isEmailValid())
                && Boolean.TRUE.equals(kakaoAccount.isEmailVerified());
    }

    /**
     * 이메일 (없으면 "noreply+kakao_{id}@asn.internal" 형태로 생성)
     * 내부 전용 도메인을 사용하여 실제 이메일과의 충돌 방지
     */
    public String email() {
        if (kakaoAccount != null && kakaoAccount.email() != null) {
            return kakaoAccount.email();
        }
        // [Fix - Medium] 내부 전용 도메인 사용으로 실제 이메일 주소와 충돌 방지
        return "noreply+kakao_" + id + "@asn.internal";
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