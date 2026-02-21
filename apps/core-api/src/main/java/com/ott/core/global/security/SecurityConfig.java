package com.ott.core.global.security;

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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ✅ @PreAuthorize 사용 가능
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ✅ CSRF 보호 활성화 (REST API는 stateless이므로 disable 가능하지만, 프로덕션에서는 고려 필요)
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        // ✅ 회원가입은 인증 불필요 (하지만 역할 제한 추가)
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                        // 비디오 임시 인증 해제
                        .requestMatchers("/api/v1/videos/**").permitAll()

                        .requestMatchers("/api/v1/interactions/**").permitAll()
                        .requestMatchers("/api/v1/bookmarks/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/search/admin/**").permitAll()
                        .requestMatchers("/api/v1/recommendations/feed/**").permitAll()

                        // 백오피스 임시 인증 해제
                        .requestMatchers("/api/v1/backoffice/**").permitAll()

                        //헬스 체크 해제
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                        // ✅ 사용자 목록 조회는 ADMIN만 가능
                        .requestMatchers(HttpMethod.GET, "/api/v1/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/role/**").hasRole("ADMIN")

                        // ✅ 관리자 전용 엔드포인트
                        .requestMatchers("/api/v1/users/*/ban").hasRole("ADMIN")
                        .requestMatchers("/api/v1/users/*/unban").hasRole("ADMIN")


                        // ✅ 나머지는 인증 필요
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}