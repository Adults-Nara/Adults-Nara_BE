package com.ott.core.modules.backoffice.controller;

import com.ott.common.persistence.enums.UserRole;
import com.ott.common.response.ApiResponse;
import com.ott.core.modules.backoffice.dto.*;
import com.ott.core.modules.backoffice.service.BackofficeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Backoffice API", description = "어드민/업로더 콘텐츠 및 유저 관리 API")
public class BackofficeController {

    private final BackofficeService backofficeService;

    @Operation(summary = "업로더 콘텐츠 목록 조회", description = "로그인한 업로더 본인의 콘텐츠 목록을 페이지네이션으로 조회합니다.")
    @GetMapping("/uploader/contents")
    @PreAuthorize("hasRole('UPLOADER')")
    public ApiResponse<PageResponse<UploaderContentResponse>> getMyContents(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<UploaderContentResponse> result = backofficeService.getUploaderContents(Long.parseLong(userId), keyword, pageable);
        return ApiResponse.success(PageResponse.from(result));
    }

    @Operation(summary = "어드민 콘텐츠 목록 조회", description = "어드민이 전체 콘텐츠 목록을 키워드 검색 및 페이지네이션으로 조회합니다.")
    @GetMapping("/admin/contents")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<AdminContentResponse>> getContents(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<AdminContentResponse> result = backofficeService.getAdminContents(keyword, pageable);
        return ApiResponse.success(PageResponse.from(result));
    }

    @Operation(summary = "콘텐츠 상세 조회", description = "업로더가 특정 콘텐츠의 상세 정보를 조회합니다.")
    @GetMapping("/contents/{videoId}")
    @PreAuthorize("hasRole('UPLOADER')")
    public ApiResponse<ContentDetailResponse> getContentDetail(
            @AuthenticationPrincipal String userId,
            @PathVariable("videoId") Long videoId
    ) {
        ContentDetailResponse response = backofficeService.getContentDetail(Long.parseLong(userId), videoId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "콘텐츠 수정", description = "업로더가 콘텐츠의 썸네일 이미지 및 메타데이터를 수정합니다.")
    @PutMapping(value = "/contents/{videoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('UPLOADER')")
    public ApiResponse<ContentUpdateResponse> updateContent(
            @AuthenticationPrincipal String userId,
            @PathVariable("videoId") Long videoId,
            @RequestPart("image") MultipartFile image,
            @RequestPart("data") ContentUpdateRequest request
    ) {
        ContentUpdateResponse response = backofficeService.updateContent(Long.parseLong(userId), videoId, image, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "콘텐츠 삭제", description = "업로더는 본인 콘텐츠만, 어드민은 모든 콘텐츠를 삭제할 수 있습니다.")
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

    @Operation(summary = "유저 목록 조회", description = "어드민이 역할(userRole) 및 키워드로 유저 목록을 페이지네이션으로 조회합니다.")
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<AdminUserResponse>> getUsers(
            @RequestParam UserRole userRole,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<AdminUserResponse> result = backofficeService.getAllUsers(userRole, keyword, pageable);
        return ApiResponse.success(PageResponse.from(result));
    }

    @Operation(summary = "유저 상태 변경", description = "어드민이 특정 유저의 활성/정지 상태를 변경합니다.")
    @PatchMapping("/users/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserStatusUpdateResponse> updateUserStatus(@RequestBody UserStatusUpdateRequest request) {
        UserStatusUpdateResponse result = backofficeService.updateUserStatus(request);
        return ApiResponse.success(result);
    }

    @Operation(summary = "유저 삭제", description = "어드민이 특정 유저를 삭제합니다.")
    @DeleteMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> deleteUser(@RequestBody DeleteUserRequest request) {
        backofficeService.deleteUser(request);
        return ApiResponse.success();
    }
}
