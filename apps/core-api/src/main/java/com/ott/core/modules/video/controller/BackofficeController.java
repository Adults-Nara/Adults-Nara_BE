/*
package com.ott.core.modules.video.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.video.dto.backoffice.UploaderContentResponse;
import com.ott.core.modules.video.service.BackofficeService;
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
@RequestMapping("/api/backoffice")
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
        long userId = Long.parseLong(authentication.getName());
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<UploaderContentResponse> result = backofficeService.getUploaderContents(userId, keyword, pageable);
        return ApiResponse.success(result);
    }
}
*/
