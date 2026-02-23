package com.ott.core.modules.auth.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.core.modules.auth.dto.KakaoTokenResponse;
import com.ott.core.modules.auth.dto.KakaoUserInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * 카카오 OAuth API와 직접 통신하는 서비스
 *
 * 역할:
 * 1. 인가코드 → 액세스토큰 교환 (POST /oauth/token)
 * 2. 액세스토큰 → 사용자 정보 조회 (GET /v2/user/me)
 */
@Slf4j
@Service
public class KakaoOAuthService {

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public KakaoOAuthService(
            WebClient.Builder webClientBuilder,
            @Value("${oauth2.kakao.client-id}") String clientId,
            @Value("${oauth2.kakao.client-secret:}") String clientSecret,
            @Value("${oauth2.kakao.redirect-uri}") String redirectUri
    ) {
        this.webClient = webClientBuilder.build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    /**
     * [Step 1] 인가코드(Authorization Code)로 카카오 액세스 토큰 교환
     */
    public KakaoTokenResponse getAccessToken(String authorizationCode) {
        log.info("[카카오 OAuth] 액세스 토큰 교환 시작");

        KakaoTokenResponse response = webClient.post()
                .uri(KAKAO_TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                        .with("client_id", clientId)
                        .with("redirect_uri", redirectUri)
                        .with("code", authorizationCode)
                        .with("client_secret", clientSecret))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                    log.error("[카카오 OAuth] 토큰 교환 실패 - 4xx 에러: {}", clientResponse.statusCode());
                    // [Fix - Medium] 에러 응답 본문은 debug 레벨로 (프로덕션에서 민감정보 노출 방지)
                    return clientResponse.bodyToMono(String.class)
                            .doOnNext(body -> log.debug("[카카오 OAuth] 에러 응답: {}", body))
                            .then(Mono.error(new BusinessException(ErrorCode.UNAUTHORIZED)));
                })
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                    log.error("[카카오 OAuth] 카카오 서버 에러: {}", clientResponse.statusCode());
                    return Mono.error(new BusinessException(ErrorCode.INTERNAL_ERROR));
                })
                .bodyToMono(KakaoTokenResponse.class)
                .block();

        if (response == null || response.accessToken() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        log.info("[카카오 OAuth] 액세스 토큰 교환 성공");
        return response;
    }

    /**
     * [Step 2] 카카오 액세스 토큰으로 사용자 정보 조회
     */
    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        log.info("[카카오 OAuth] 사용자 정보 조회 시작");

        KakaoUserInfoResponse response = webClient.get()
                .uri(KAKAO_USER_INFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                    log.error("[카카오 OAuth] 사용자 정보 조회 실패: {}", clientResponse.statusCode());
                    return Mono.error(new BusinessException(ErrorCode.UNAUTHORIZED));
                })
                .bodyToMono(KakaoUserInfoResponse.class)
                .block();

        if (response == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        log.info("[카카오 OAuth] 사용자 정보 조회 성공 - kakaoId: {}, email: {}", response.id(), response.email());
        return response;
    }
}