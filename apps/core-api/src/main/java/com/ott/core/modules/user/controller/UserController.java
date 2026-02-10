package com.ott.core.modules.user.controller;

import com.ott.common.persistence.enums.UserRole;
import com.ott.common.response.ApiResponse;
import com.ott.core.modules.user.dto.request.CreateUserRequest;
import com.ott.core.modules.user.dto.request.UpdateUserRequest;
import com.ott.core.modules.user.dto.request.BanUserRequest;
import com.ott.core.modules.user.dto.response.UserDetailResponse;
import com.ott.core.modules.user.dto.response.UserResponse;
import com.ott.core.modules.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 일반 회원가입 (VIEWER만 생성)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ApiResponse.success(response);
    }

    /**
     * 업로더 회원가입 (초대 코드 필요)
     */
    @PostMapping("/uploader")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createUploader(
            @Valid @RequestBody CreateUserRequest request,
            @RequestParam String inviteCode
    ) {
        UserResponse response = userService.createUploader(request, inviteCode);
        return ApiResponse.success(response);
    }

    /**
     * 사용자 목록 조회 (ADMIN만 가능)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ApiResponse.success(users);
    }

    /**
     * 역할별 사용자 목록 조회 (ADMIN만 가능)
     */
    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<UserResponse>> getUsersByRole(
            @PathVariable UserRole role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<UserResponse> users = userService.getUsersByRole(role, pageable);
        return ApiResponse.success(users);
    }

    /**
     * 사용자 상세 조회 (본인 또는 ADMIN)
     */
    @GetMapping("/{userId}")
    @PreAuthorize("authentication.principal == #userId.toString() or hasRole('ADMIN')")
    public ApiResponse<UserDetailResponse> getUserDetail(@PathVariable Long userId) {
        UserDetailResponse response = userService.getUserDetail(userId);
        return ApiResponse.success(response);
    }

    /**
     * 사용자 정보 수정 (본인만 가능)
     */
    @PatchMapping("/{userId}")
    @PreAuthorize("authentication.principal == #userId.toString()")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UserResponse response = userService.updateUser(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * 사용자 정지 (ADMIN만 가능)
     */
    @PostMapping("/{userId}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> banUser(
            @PathVariable Long userId,
            @Valid @RequestBody BanUserRequest request,
            Authentication authentication  // ✅ SecurityContext에서 안전하게 가져옴
    ) {
        Long adminId = Long.parseLong(authentication.getName());
        userService.banUser(userId, request, adminId);
        return ApiResponse.success();
    }

    /**
     * 사용자 정지 해제 (ADMIN만 가능)
     */
    @PostMapping("/{userId}/unban")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> unbanUser(@PathVariable Long userId) {
        userService.unbanUser(userId);
        return ApiResponse.success();
    }

    /**
     * 사용자 삭제 (본인 또는 ADMIN)
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("authentication.principal == #userId.toString() or hasRole('ADMIN')")
    public ApiResponse<?> deleteUser(
            @PathVariable Long userId,
            @RequestParam String reason
    ) {
        userService.deleteUser(userId, reason);
        return ApiResponse.success();
    }

    /**
     * 사용자 비활성화 (본인만 가능)
     */
    @PostMapping("/{userId}/deactivate")
    @PreAuthorize("authentication.principal == #userId.toString()")
    public ApiResponse<?> deactivateUser(@PathVariable Long userId) {
        userService.deactivateUser(userId);
        return ApiResponse.success();
    }
}