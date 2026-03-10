package com.ott.core.modules.user.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.user.dto.request.UpdateUserRequest;
import com.ott.core.modules.user.dto.response.UserDetailResponse;
import com.ott.core.modules.user.dto.response.UserResponse;
import com.ott.core.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 일반 사용자(VIEWER) 본인 전용 API
 *
 * - 모든 엔드포인트는 JWT 인증 객체에서 userId를 추출합니다.
 * - URL PathVariable로 userId를 받지 않으므로 타 사용자 리소스 접근이 원천 차단됩니다.
 * - ADMIN 전용 사용자 관리(목록 조회, 역할 조회, 정지/탈퇴 등)는 BackofficeController에서 처리합니다.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "유저 API", description = "로그인 사용자 본인 정보 조회 및 수정 API")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "내 정보 조회",
            description = "로그인한 사용자 본인의 상세 정보를 조회합니다."
    )
    @GetMapping("/me")
    public ApiResponse<UserDetailResponse> getMyDetail(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ApiResponse.success(userService.getUserDetail(userId));
    }

    @Operation(
            summary = "내 정보 수정",
            description = "닉네임, 비밀번호, 프로필 이미지, 관심 태그를 수정합니다. null 필드는 수정하지 않습니다."
    )
    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMyInfo(
            Authentication authentication,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        Long userId = extractUserId(authentication);
        return ApiResponse.success(userService.updateUser(userId, request));
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "본인 계정을 Soft Delete 처리합니다."
    )
    @DeleteMapping("/me")
    public ApiResponse<?> deleteMyAccount(
            Authentication authentication,
            @RequestParam String reason
    ) {
        Long userId = extractUserId(authentication);
        userService.deleteUser(userId, reason);
        return ApiResponse.success();
    }

    @Operation(
            summary = "계정 비활성화",
            description = "본인 계정을 일시적으로 비활성화합니다. 다음 로그인 시 자동으로 활성화됩니다."
    )
    @PostMapping("/me/deactivate")
    public ApiResponse<?> deactivateMyAccount(Authentication authentication) {
        Long userId = extractUserId(authentication);
        userService.deactivateUser(userId);
        return ApiResponse.success();
    }

    // ====== Private Methods ======

    private Long extractUserId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}