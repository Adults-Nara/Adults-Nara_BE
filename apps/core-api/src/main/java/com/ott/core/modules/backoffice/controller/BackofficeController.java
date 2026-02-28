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
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/backoffice")
public class BackofficeController {

    private final BackofficeService backofficeService;

    @GetMapping("/uploader/contents")
    @PreAuthorize("hasRole('UPLOADER')")
    public ApiResponse<Page<UploaderContentResponse>> getMyContents(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<UploaderContentResponse> result = backofficeService.getUploaderContents(Long.parseLong(userId), keyword, pageable);
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

    @GetMapping("/contents/{videoId}")
    @PreAuthorize("hasRole('UPLOADER')")
    public ApiResponse<ContentDetailResponse> getContentDetail(
            @AuthenticationPrincipal String userId,
            @PathVariable("videoId") Long videoId
    ) {
        ContentDetailResponse response = backofficeService.getContentDetail(Long.parseLong(userId), videoId);
        return ApiResponse.success(response);
    }

    @PutMapping(value = "/contents/{videoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('UPLOADER')")
    public ApiResponse<ContentUpdateResponse> updateContent(
            @AuthenticationPrincipal String userId,
            @PathVariable("videoId") Long videoId,
            @RequestPart("image") MultipartFile image,
            @RequestPart ContentUpdateRequest request
    ) {
        ContentUpdateResponse response = backofficeService.updateContent(Long.parseLong(userId), videoId, image, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/contents")
    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    public ApiResponse<?> deleteContent(
            Authentication authentication,
            @AuthenticationPrincipal String userId,
            @RequestBody ContentDeleteRequest request
    ) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        backofficeService.deleteContent(Long.parseLong(userId), isAdmin, request);
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
