package com.ott.core.modules.bookmark.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.bookmark.dto.BookmarkStatusResponseDto;
import com.ott.core.modules.bookmark.service.BookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "북마크 API", description = "찜하기 기능")
@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    // 찜하기
    @Operation(summary = "찜하기(북마크) 토글", description = "누를 때마다 찜 상태가 켜지거나 꺼집니다.")
    @PostMapping("/{videoId}")
    public ResponseEntity<ApiResponse<?>> toggleBookmark(
            @PathVariable("videoId") Long videoId,
            @RequestParam Long userId) {

        bookmarkService.toggleBookmark(userId, videoId);

        return ResponseEntity.ok(ApiResponse.success());
    }

    // 북마크 여부 조회
    @Operation(summary = "북마크 여부 조회", description = "내가 이 영상을 찜했는지 확인합니다.")
    @GetMapping("/{videoId}/status")
    public ResponseEntity<ApiResponse<BookmarkStatusResponseDto>> getBookmarkStatus(
            @PathVariable("videoId") Long videoId,
            @RequestParam Long userId) {

        boolean status = bookmarkService.isBookmarked(userId, videoId);
        BookmarkStatusResponseDto responseDto = BookmarkStatusResponseDto.from(status);

        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }
}