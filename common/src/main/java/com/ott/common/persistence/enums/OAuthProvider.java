package com.ott.common.persistence.enums;

public enum OAuthProvider {
    KAKAO;

    public static OAuthProvider from(String provider) {
        return OAuthProvider.valueOf(provider.toUpperCase());
    }
}