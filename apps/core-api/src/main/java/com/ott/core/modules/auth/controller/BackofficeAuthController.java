package com.ott.core.modules.auth.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.auth.dto.BackofficeLoginRequest;
import com.ott.core.modules.auth.dto.BackofficeLoginResponse;
import com.ott.core.modules.auth.dto.BackofficeSignupRequest;
import com.ott.core.modules.auth.service.BackofficeAuthService;
import com.ott.core.modules.user.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/backoffice/auth")
@RequiredArgsConstructor
@Tag(name = "백오피스 인증 API", description = "업로더/관리자 로그인 및 회원가입")
public class BackofficeAuthController {

    private final BackofficeAuthService backofficeAuthService;

    /**
     * 백오피스 로그인 (이메일 + 비밀번호)
     * UPLOADER 또는 ADMIN만 로그인 가능
     */
    @Operation(
            summary = "백오피스 로그인",
            description = "이메일과 비밀번호로 백오피스에 로그인합니다. UPLOADER 또는 ADMIN 계정만 가능합니다."
    )
    @PostMapping("/login")
    public ApiResponse<BackofficeLoginResponse> login(
            @Valid @RequestBody BackofficeLoginRequest request
    ) {
        BackofficeLoginResponse response = backofficeAuthService.login(request);
        return ApiResponse.success(response);
    }

    /**
     * 업로더 회원가입 (초대코드 필요)
     */
    @Operation(
            summary = "업로더 회원가입",
            description = "초대코드를 통해 업로더 계정을 생성합니다. 이메일 중복 체크를 수행합니다."
    )
    @PostMapping("/signup/uploader")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> signupUploader(
            @Valid @RequestBody BackofficeSignupRequest request,
            @RequestParam String inviteCode
    ) {
        UserResponse response = backofficeAuthService.signupUploader(request, inviteCode);
        return ApiResponse.success(response);
    }

    /**
     * 업로더 계정 탈퇴 (Soft Delete)
     * 본인만 탈퇴 가능
     */
    @Operation(
            summary = "업로더 계정 탈퇴",
            description = "업로더 본인이 계정을 탈퇴합니다. Soft Delete로 처리됩니다."
    )
    @DeleteMapping("/account")
    @PreAuthorize("hasRole('UPLOADER')")
    public ApiResponse<?> deleteAccount(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        backofficeAuthService.deleteUploaderAccount(userId);
        return ApiResponse.success();
    }

    /**
     * 이메일 중복 체크
     */
    @Operation(
            summary = "이메일 중복 체크",
            description = "회원가입 전 이메일 사용 가능 여부를 확인합니다."
    )
    @GetMapping("/check-email")
    public ApiResponse<Boolean> checkEmailAvailable(@RequestParam String email) {
        // true = 사용 가능 (중복 없음), false = 이미 존재
        boolean available = !backofficeAuthService.isEmailExists(email);
        return ApiResponse.success(available);
    }
}