package com.ott.core.modules.backoffice.controller;

import com.ott.common.persistence.enums.UserRole;
import com.ott.common.response.ApiResponse;
import com.ott.core.modules.backoffice.dto.*;
import com.ott.core.modules.backoffice.service.BackofficeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/backoffice")
public class BackofficeController {

    private final BackofficeService backofficeService;

    @GetMapping("/uploader/contents")
    @PreAuthorize("hasRole('UPLOADER')")
    public ApiResponse<Page<UploaderContentResponse>> getMyContents(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        long userId = Long.parseLong(authentication.getName()); // 수정 필요
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<UploaderContentResponse> result = backofficeService.getUploaderContents(userId, keyword, pageable);
        return ApiResponse.success(result);
    }

    @GetMapping("/admin/contents")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<AdminContentResponse>> getContents(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<AdminContentResponse> result = backofficeService.getAdminContents(keyword, pageable);
        return ApiResponse.success(result);
    }

    @GetMapping("/contents/{videoMetadataId}")
    @PreAuthorize("hasRole('UPLOADER')")
    public ApiResponse<ContentDetailResponse> getContentDetail(
            Authentication authentication,
            @PathVariable("videoMetadataId") Long videoMetadataId
    ) {
        long userId = Long.parseLong(authentication.getName()); // 수정 필요
        ContentDetailResponse response = backofficeService.getContentDetail(userId, videoMetadataId);
        return ApiResponse.success(response);
    }

    @PutMapping("/contents/{videoMetadataId}")
    @PreAuthorize("hasRole('UPLOADER')")
    public ApiResponse<ContentUpdateResponse> updateContent(
            Authentication authentication,
            @PathVariable("videoMetadataId") Long videoMetadataId,
            @RequestBody ContentUpdateRequest request
    ) {
        long userId = Long.parseLong(authentication.getName()); // 수정 필요
        ContentUpdateResponse response = backofficeService.updateContent(userId, videoMetadataId, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/contents")
    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    public ApiResponse<?> deleteContent(
            Authentication authentication,
            @RequestBody ContentDeleteRequest request
    ) {
        long userId = Long.parseLong(authentication.getName()); // 수정 필요

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        backofficeService.deleteContent(userId, isAdmin, request);
        return ApiResponse.success();
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<AdminUserResponse>> getUsers(
            @RequestParam UserRole userRole,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<AdminUserResponse> result = backofficeService.getAllUsers(userRole, keyword, pageable);
        return ApiResponse.success(result);
    }

    @PatchMapping("/users/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserStatusUpdateResponse> updateUserStatus(@RequestBody UserStatusUpdateRequest request) {
        UserStatusUpdateResponse result = backofficeService.updateUserStatus(request);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> deleteUser(@RequestBody DeleteUserRequest request) {
        backofficeService.deleteUser(request);
        return ApiResponse.success();
    }
}
