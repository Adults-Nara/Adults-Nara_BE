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
    // [Fix] CustomAccessDeniedHandler에 @Component가 반드시 있어야 합니다.
    // 없으면 Bean을 찾을 수 없어 앱이 시작되지 않습니다.
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

                // [Fix #4] state 파라미터 검증을 위해 세션 허용 (ALWAYS가 아닌 IF_REQUIRED)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 403 에러 핸들링
                .exceptionHandling(exception ->
                        exception.accessDeniedHandler(customAccessDeniedHandler)
                )

                .authorizeHttpRequests(auth -> auth
                        // ===== 인증 불필요 (Public) =====

                        // 카카오 OAuth 엔드포인트
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // 회원가입
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()

                        // 비디오 (임시 인증 해제)
                        .requestMatchers("/api/v1/videos/**").permitAll()

                        .requestMatchers("/api/v1/interactions/**").permitAll()
                        .requestMatchers("/api/v1/bookmarks/**").permitAll()

                        // Swagger
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // [Fix - Medium] health endpoint는 기본 경로만 허용 (상세 정보 노출 방지)
                        .requestMatchers("/actuator/health").permitAll()

                        // 검색/추천 (임시 인증 해제)
                        .requestMatchers("/api/v1/search/admin/**").permitAll()
                        .requestMatchers("/api/v1/recommendations/feed/**").permitAll()

                        // 백오피스 (임시 인증 해제)
                        .requestMatchers("/api/v1/backoffice/**").permitAll()

                        // ===== ADMIN 전용 =====
                        .requestMatchers(HttpMethod.GET, "/api/v1/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/role/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/users/*/ban").hasRole("ADMIN")
                        .requestMatchers("/api/v1/users/*/unban").hasRole("ADMIN")

                        // ===== 나머지는 인증 필요 =====
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    /**
     * CORS 설정
     * React 프론트엔드(localhost:3000)에서의 요청을 허용합니다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin (프론트엔드 주소)
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173"  // Vite 기본 포트
        ));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 허용할 헤더
        configuration.setAllowedHeaders(List.of("*"));

        // 인증정보(쿠키, Authorization 헤더) 포함 허용
        configuration.setAllowCredentials(true);

        // 프론트에서 접근 가능한 응답 헤더
        configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}