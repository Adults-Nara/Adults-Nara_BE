package com.ott.core.global.security;

import com.ott.core.global.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .csrf(csrf -> csrf.disable())

                // 완전 무상태 - JWT 인증이므로 세션/JSESSIONID 미생성
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // JWT 인증 필터
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 403 에러 핸들링
                .exceptionHandling(exception ->
                        exception.accessDeniedHandler(customAccessDeniedHandler)
                )

                .authorizeHttpRequests(auth -> auth

                        // ===================================================================
                        // 1. 완전 공개 (Public) - 비로그인 사용자도 접근 가능
                        // ===================================================================

                        // --- 카카오 OAuth 인증 ---
                        .requestMatchers("/api/v1/auth/kakao/**").permitAll()
                        .requestMatchers("/api/v1/auth/token/refresh").permitAll()

                        // --- 백오피스 인증 (로그인/회원가입/이메일체크) ---
                        .requestMatchers("/api/v1/backoffice/auth/login").permitAll()
                        .requestMatchers("/api/v1/backoffice/auth/signup/**").permitAll()
                        .requestMatchers("/api/v1/backoffice/auth/check-email").permitAll()

                        // --- 비디오 (비로그인 시청 가능) ---
                        .requestMatchers("/api/v1/videos/*/play").permitAll()

                        // --- 검색/추천 (비로그인 사용 가능) ---
                        .requestMatchers("/api/v1/search/**").permitAll()
                        .requestMatchers("/api/v1/recommendations/**").permitAll()

                        // --- 좋아요/북마크 (비로그인도 조회 가능하도록) ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/interactions/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookmarks/**").permitAll()

                        // --- Swagger ---
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // --- Health Check ---
                        .requestMatchers("/actuator/health").permitAll()

                        // ===================================================================
                        // 2. 인증 필요 (로그인 사용자)
                        // ===================================================================

                        // --- 현재 사용자 정보 조회 (로그인 필수) ---
                        .requestMatchers("/api/v1/auth/me").authenticated()

                        // --- 좋아요/북마크 (쓰기: 로그인 필수) ---
                        .requestMatchers(HttpMethod.POST, "/api/v1/interactions/**").authenticated()
//                        .requestMatchers(HttpMethod.POST, "/api/v1/bookmarks/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/bookmarks/**").permitAll()

                        // --- 사용자 프로필 수정 (본인만 가능 - @PreAuthorize로 세부 제어) ---
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/users/{userId}").authenticated()
                        .requestMatchers("/api/v1/users/{userId}/deactivate").authenticated()

                        // --- 시청 통계 (로그인 필수) ---
                        .requestMatchers("/api/v1/stats/me/**").authenticated()

                        // ===================================================================
                        // 3. UPLOADER 전용
                        // ===================================================================

                        // --- 업로더 계정 탈퇴 ---
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/backoffice/auth/account").hasRole("UPLOADER")

                        // --- 업로더 컨텐츠 관리 ---
                        .requestMatchers("/api/v1/backoffice/uploader/**").hasRole("UPLOADER")

                        // ===================================================================
                        // 4. ADMIN 전용
                        // ===================================================================

                        // --- 사용자 관리 ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/role/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/users/*/ban").hasRole("ADMIN")
                        .requestMatchers("/api/v1/users/*/unban").hasRole("ADMIN")

                        // --- 특정 유저 시청 통계 조회 (관리자) ---
                        .requestMatchers("/api/v1/stats/*/**").hasRole("ADMIN")

                        // --- 관리자 백오피스 (전체 영상 접근, 유저 제재) ---
                        .requestMatchers("/api/v1/backoffice/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/backoffice/users/**").hasRole("ADMIN")

                        // ===================================================================
                        // 5. UPLOADER 또는 ADMIN
                        // ===================================================================
                        .requestMatchers("/api/v1/backoffice/contents/**").hasAnyRole("UPLOADER", "ADMIN")

                        // ===================================================================
                        // 6. 나머지는 인증 필요
                        // ===================================================================
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    /**
     * CORS 설정
     * Set-Cookie는 CloudFront Signed Cookie(비디오 재생)에 필요하므로 유지
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://asinna.store",
                "https://admin.asinna.store"
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}