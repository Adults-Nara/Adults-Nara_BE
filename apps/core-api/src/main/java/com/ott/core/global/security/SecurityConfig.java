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

                        .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                        .exceptionHandling(
                                exception -> exception.accessDeniedHandler(customAccessDeniedHandler))

                        .authorizeHttpRequests(auth -> auth

                                // ===================================================================
                                // ADMIN 전용
                                // ===================================================================

                                // --- 사용자 관리 ---
                                .requestMatchers(HttpMethod.GET, "/api/v1/users").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/v1/users/role/**").hasRole("ADMIN")
                                .requestMatchers("/api/v1/users/*/ban").hasRole("ADMIN")
                                .requestMatchers("/api/v1/users/*/unban").hasRole("ADMIN")

                                // --- 관리자 백오피스 ---
                                .requestMatchers("/api/v1/backoffice/admin/**").hasRole("ADMIN")
                                .requestMatchers("/api/v1/backoffice/users/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/v1/bookmarks/admin/warmup").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/v1/interactions/admin/warmup").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/v1/search/admin/sync").hasRole("ADMIN")

                                // ===================================================================
                                // 1. 완전 공개 (Public)
                                // ===================================================================

                                // --- 카카오 OAuth 인증 ---
                                .requestMatchers("/api/v1/auth/kakao/**").permitAll()
                                .requestMatchers("/api/v1/auth/token/refresh").permitAll()

                                .requestMatchers("/api/v1/auth/onboarding/complete").authenticated()

                                // --- 백오피스 인증 (로그인/회원가입/이메일체크/토큰갱신) ---
                                // 중복 설정 제거 후 아래 한 곳에서만 관리
                                .requestMatchers("/api/v1/backoffice/auth/login").permitAll()
                                .requestMatchers("/api/v1/backoffice/auth/signup/**").permitAll()
                                .requestMatchers("/api/v1/backoffice/auth/check-email").permitAll()
                                .requestMatchers("/api/v1/backoffice/auth/token/refresh").permitAll()
                                .requestMatchers("/api/v1/backoffice/auth/logout").authenticated()

                                // --- 비디오 ---
                                .requestMatchers("/api/v1/videos/*/play").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/videos/*").permitAll()

                                // --- 태그별 영상 목록 ---
                                .requestMatchers(HttpMethod.GET, "/api/v1/tags/*/videos").permitAll()

                                // --- 광고 ---
                                .requestMatchers("/api/v1/ads").permitAll()

                                // --- 검색/추천 ---
                                .requestMatchers("/api/v1/search/**").permitAll()
                                .requestMatchers("/api/v1/recommendations/**").permitAll()

                                // --- 좋아요/북마크/랭킹 ---
                                .requestMatchers(HttpMethod.GET, "/api/v1/interactions/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/bookmarks/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/ranking/**").permitAll()

                                // --- 댓글 목록 조회 --- (순서 변경 금지)
                                .requestMatchers(HttpMethod.GET, "/api/v1/comment/videos/*/me").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/v1/comment/videos/**").permitAll()

                                // --- Swagger ---
                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                                // --- Health Check ---
                                .requestMatchers("/actuator/health").permitAll()

                                // --- UPlus ---
                                .requestMatchers(HttpMethod.GET, "/api/v1/uplus/plans").permitAll()
                                .requestMatchers("/api/v1/uplus/**").authenticated()

                                // ===================================================================
                                // 2. 인증 필요 (로그인 사용자)
                                // ===================================================================

                                .requestMatchers("/api/v1/auth/me").authenticated()

                                .requestMatchers(HttpMethod.POST, "/api/v1/interactions/**").authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/v1/bookmarks/**").authenticated()

                                .requestMatchers(HttpMethod.PATCH, "/api/v1/users/{userId}").authenticated()
                                .requestMatchers("/api/v1/users/{userId}/deactivate").authenticated()

                                // ===================================================================
                                // 3. UPLOADER 전용
                                // ===================================================================

                                .requestMatchers(HttpMethod.DELETE, "/api/v1/backoffice/auth/account").hasRole("UPLOADER")
                                .requestMatchers("/api/v1/backoffice/uploader/**").hasRole("UPLOADER")

                                // ===================================================================
                                // 4. UPLOADER 또는 ADMIN
                                // ===================================================================

                                .requestMatchers("/api/v1/backoffice/contents/**").hasAnyRole("UPLOADER", "ADMIN")

                                // ===================================================================
                                // 5. 나머지는 인증 필요
                                // ===================================================================
                                .anyRequest().authenticated());

                return http.build();
        }

        /**
         * CORS 설정
         * Set-Cookie는 CloudFront Signed Cookie(비디오 재생)에 필요하므로 유지
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOriginPatterns(List.of(
                        "http://localhost:*",
                        "https://localhost:*",
                        "http://*.asinna.store",
                        "https://*.asinna.store"
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