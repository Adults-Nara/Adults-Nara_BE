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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 사용자 생성 (회원가입)
     * POST /api/v1/users
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ApiResponse.success(response);
    }

    /**
     * 사용자 목록 조회 (페이징)
     * GET /api/v1/users?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping
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
     * 역할별 사용자 목록 조회
     * GET /api/v1/users/role/VIEWER?page=0&size=10
     */
    @GetMapping("/role/{role}")
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
     * 사용자 상세 조회
     * GET /api/v1/users/{userId}
     */
    @GetMapping("/{userId}")
    public ApiResponse<UserDetailResponse> getUserDetail(@PathVariable Long userId) {
        UserDetailResponse response = userService.getUserDetail(userId);
        return ApiResponse.success(response);
    }

    /**
     * 사용자 정보 수정
     * PATCH /api/v1/users/{userId}
     */
    @PatchMapping("/{userId}")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UserResponse response = userService.updateUser(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * 사용자 정지 (관리자 전용)
     * POST /api/v1/users/{userId}/ban?adminId=999
     */
    @PostMapping("/{userId}/ban")
    public ApiResponse<?> banUser(
            @PathVariable Long userId,
            @Valid @RequestBody BanUserRequest request,
            @RequestParam Long adminId
    ) {
        userService.banUser(userId, request, adminId);
        return ApiResponse.success();
    }

    /**
     * 사용자 정지 해제 (관리자 전용)
     * POST /api/v1/users/{userId}/unban
     */
    @PostMapping("/{userId}/unban")
    public ApiResponse<?> unbanUser(@PathVariable Long userId) {
        userService.unbanUser(userId);
        return ApiResponse.success();
    }

    /**
     * 사용자 삭제 (소프트 삭제)
     * DELETE /api/v1/users/{userId}?reason=회원 탈퇴 요청
     */
    @DeleteMapping("/{userId}")
    public ApiResponse<?> deleteUser(
            @PathVariable Long userId,
            @RequestParam String reason
    ) {
        userService.deleteUser(userId, reason);
        return ApiResponse.success();
    }

    /**
     * 사용자 비활성화 (본인 요청)
     * POST /api/v1/users/{userId}/deactivate
     */
    @PostMapping("/{userId}/deactivate")
    public ApiResponse<?> deactivateUser(@PathVariable Long userId) {
        userService.deactivateUser(userId);
        return ApiResponse.success();
    }
}