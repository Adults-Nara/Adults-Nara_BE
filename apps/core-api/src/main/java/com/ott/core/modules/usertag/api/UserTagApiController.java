package com.ott.core.modules.usertag.api;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.usertag.dto.OnboardingTagRequest;
import com.ott.core.modules.usertag.dto.UpdateUserTagRequest;
import com.ott.core.modules.usertag.service.UserTagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user-tag")
public class UserTagApiController {

    private final UserTagService userTagService;

    @PostMapping("/onboarding")
    public ApiResponse<?> saveOnboardingTags(@AuthenticationPrincipal String userId, @Valid @RequestBody OnboardingTagRequest request) {
        userTagService.saveOnboardingTags(Long.parseLong(userId), request.tagIds());
        return ApiResponse.success();
    }

    @PutMapping
    public ApiResponse<?> updateUserTags(@AuthenticationPrincipal String userId, @RequestBody UpdateUserTagRequest request) {
        userTagService.updateUserTags(Long.parseLong(userId), request.tagIds());
        return ApiResponse.success();
    }
}
