package com.ott.core.modules.user.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.user.dto.request.UpdateUserRequest;
import com.ott.core.modules.user.dto.response.UserDetailResponse;
import com.ott.core.modules.user.dto.response.UserResponse;
import com.ott.core.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "유저 API", description = "사용자 정보 조회 및 수정 API")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "사용자 상세 조회", description = "본인 또는 관리자가 사용자 상세 정보를 조회합니다.")
    @GetMapping("/{userId}")
    @PreAuthorize("authentication.principal == #userId.toString() or hasRole('ADMIN')")
    public ApiResponse<UserDetailResponse> getUserDetail(@PathVariable Long userId) {
        UserDetailResponse response = userService.getUserDetail(userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "사용자 정보 수정", description = "본인만 닉네임, 비밀번호, 프로필 이미지, 관심 태그를 수정할 수 있습니다.")
    @PatchMapping("/{userId}")
    @PreAuthorize("authentication.principal == #userId.toString()")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UserResponse response = userService.updateUser(userId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "사용자 탈퇴", description = "본인 또는 관리자가 사용자를 삭제(soft delete)합니다.")
    @DeleteMapping("/{userId}")
    @PreAuthorize("authentication.principal == #userId.toString() or hasRole('ADMIN')")
    public ApiResponse<?> deleteUser(
            @PathVariable Long userId,
            @RequestParam String reason
    ) {
        userService.deleteUser(userId, reason);
        return ApiResponse.success();
    }

    @Operation(summary = "사용자 비활성화", description = "본인이 계정을 일시적으로 비활성화합니다.")
    @PostMapping("/{userId}/deactivate")
    @PreAuthorize("authentication.principal == #userId.toString()")
    public ApiResponse<?> deactivateUser(@PathVariable Long userId) {
        userService.deactivateUser(userId);
        return ApiResponse.success();
    }
}