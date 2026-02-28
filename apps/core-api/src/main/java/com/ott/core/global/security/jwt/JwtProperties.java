package com.ott.core.global.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT 서명에 사용할 비밀키 (Base64 인코딩된 값)
     * application.yml의 jwt.secret 값이 바인딩됩니다.
     */
    private String secret;

    /**
     * Access Token 만료 시간 (밀리초)
     */
    private long accessTokenExpiration = 36000000L;

    /**
     * Refresh Token 만료 시간 (밀리초)
     */
    private long refreshTokenExpiration = 7776000000L;
}