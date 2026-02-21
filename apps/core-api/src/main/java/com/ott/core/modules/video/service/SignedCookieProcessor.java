package com.ott.core.modules.video.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SignedCookieProcessor {
    private final String cloudFrontDomain; // 예: d111111abcdef8.cloudfront.net (프로토콜/경로 제외)
    private final String keyPairId;         // CloudFront Public Key ID (또는 signer id)
    private final PrivateKey privateKey;

    public SignedCookieProcessor(@Value("${aws.cloudfront.domain}") String cloudFrontDomain,
                                 @Value("${aws.cloudfront.keypair-id}") String keyPairId,
                                 @Value("${aws.cloudfront.private-key-pem-b64}") String privateKeyB64) {
        this.cloudFrontDomain = cloudFrontDomain;
        this.keyPairId = keyPairId;
        String privateKeyPem = new String(Base64.getDecoder().decode(privateKeyB64), StandardCharsets.UTF_8);
        this.privateKey = loadPkcs8PrivateKey(privateKeyPem);
    }

    /**
     * videoId 폴더(또는 특정 prefix) 아래 모든 파일 접근을 허용하는 Signed Cookies 발급.
     *
     * @param resourcePathPrefix 예: "/videos/{videoId}/outputs/hls/v1/" (앞에 / 포함 권장)
     * @param ttl                예: Duration.ofMinutes(30)
     * @return cookieName -> cookieValue (3개)
     */
    public Map<String, String> createSignedCookies(String resourcePathPrefix, Duration ttl) {
        return createSignedCookies(resourcePathPrefix, ttl, null);
    }


    public Map<String, String> createSignedCookies(String resourcePathPrefix, Duration ttl, String ipCidr) {
        String prefix = normalizePrefix(resourcePathPrefix);

        // CloudFront custom policy는 Resource에 wildcard를 쓸 수 있어 "폴더 하위 전체" 접근이 가능
        // 예: https://dxxx.cloudfront.net/videos/123/outputs/hls/v1/*
        String resource = "https://" + cloudFrontDomain + prefix + "*";

        long expiresEpoch = Instant.now().plus(ttl).getEpochSecond();

        String policyJson = buildCustomPolicy(resource, expiresEpoch, ipCidr);

        // 1) Policy: 공백 없는 JSON을 Base64 인코딩 후, CloudFront용 URL-safe 변환
        String policyB64 = toCloudFrontSafeBase64(policyJson.getBytes(StandardCharsets.UTF_8));

        // 2) Signature: (원본 policy bytes)를 RSA로 서명 → Base64 → CloudFront용 URL-safe 변환
        // CloudFront는 전통적으로 RSA-SHA1을 요구/사용해왔고(환경에 따라 제약), 최신엔 ECDSA도 지원 안내가 있음.
        // 여기서는 가장 호환성이 높은 RSA-SHA1 예시로 구현. :contentReference[oaicite:3]{index=3}
        byte[] sig = signRsaSha1(policyJson.getBytes(StandardCharsets.UTF_8), privateKey);
        String sigB64 = toCloudFrontSafeBase64(sig);

        Map<String, String> cookies = new LinkedHashMap<>();
        cookies.put("CloudFront-Policy", policyB64);
        cookies.put("CloudFront-Signature", sigB64);
        cookies.put("CloudFront-Key-Pair-Id", keyPairId);
        return cookies;
    }

    /**
     * Spring에서 Set-Cookie로 내려주고 싶다면 ResponseCookie로 만들어주는 헬퍼.
     * (도메인/Path/Secure/HttpOnly/SameSite 등은 서비스 성격에 맞춰 조정)
     */
    public Map<String, ResponseCookie> toSetCookieHeaders(
            Map<String, String> cookieValues,
            String cookieDomain,
            String cookiePath,
            Duration maxAge,
            boolean secure,
            boolean httpOnly
    ) {
        Map<String, ResponseCookie> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : cookieValues.entrySet()) {
            ResponseCookie c = ResponseCookie.from(e.getKey(), e.getValue())
                    .domain(cookieDomain)     // 예: ".yourdomain.com" 또는 CloudFront 도메인
                    .path(cookiePath)         // 보통 "/" 또는 "/videos/{videoId}/"
                    .maxAge(maxAge)
                    .secure(secure)
                    .httpOnly(httpOnly)
                    .sameSite("None")      // 크로스사이트(CloudFront 도메인)면 None+Secure 필요할 수 있음
                    .build();
            out.put(e.getKey(), c);
        }
        return out;
    }


    // -------------------- 내부 구현 --------------------

    private String buildCustomPolicy(String resource, long expiresEpochSeconds, String ipCidr) {
        // AWS 문서 형식: Statement 배열, Resource + Condition(DateLessThan, (옵션)IpAddress)
        // 공백을 최소화하기 위해 문자열을 직접 구성(간단/안전)
        // :contentReference[oaicite:4]{index=4}
        StringBuilder sb = new StringBuilder();
        sb.append("{\"Statement\":[{\"Resource\":\"")
                .append(escapeJson(resource))
                .append("\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":")
                .append(expiresEpochSeconds)
                .append("}");

        if (ipCidr != null && !ipCidr.isBlank()) {
            sb.append(",\"IpAddress\":{\"AWS:SourceIp\":\"")
                    .append(escapeJson(ipCidr.trim()))
                    .append("\"}");
        }

        sb.append("}}]}");
        return sb.toString();
    }

    private byte[] signRsaSha1(byte[] data, PrivateKey pk) {
        try {
            Signature s = Signature.getInstance("SHA1withRSA");
            s.initSign(pk);
            s.update(data);
            return s.sign();
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign policy (SHA1withRSA)", e);
        }
    }

    private String toCloudFrontSafeBase64(byte[] bytes) {
        String b64 = Base64.getEncoder().encodeToString(bytes);
        // CloudFront “URL-safe” 변환 규칙: + -> -, = -> _, / -> ~
        // (AWS 문서/예제에서 널리 쓰는 방식)
        return b64.replace('+', '-')
                .replace('=', '_')
                .replace('/', '~');
    }

    private PrivateKey loadPkcs8PrivateKey(String pem) {
        try {
            // -----BEGIN PRIVATE KEY-----  (PKCS#8) 형태를 가정
            String normalized = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] der = Base64.getDecoder().decode(normalized);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to load private key. Ensure it's PKCS#8 PEM (BEGIN PRIVATE KEY).", e
            );
        }
    }

    private String normalizePrefix(String p) {
        String x = (p == null) ? "" : p.trim();
        if (!x.startsWith("/")) x = "/" + x;
        // prefix가 파일명으로 끝나면(예: master.m3u8) 폴더 접근용으로는 끝에 /를 보장하는 게 보통 편함
        if (!x.endsWith("/")) x = x + "/";
        return x;
    }

    private String escapeJson(String s) {
        // 최소 이스케이프만
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String must(String v, String name) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(name + " is required");
        return v.trim();
    }
}
