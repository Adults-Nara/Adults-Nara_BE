package com.ott.core.modules.tag.api;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.tag.dto.response.ChildTagResponse;
import com.ott.core.modules.tag.dto.response.ParentTagWithChildResponse;
import com.ott.core.modules.tag.dto.response.TagVideoResponse;
import com.ott.core.modules.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tags")
public class TagApiController {

    private final TagService tagService;

    @GetMapping("/my-child-tags")
    public ApiResponse<List<ChildTagResponse>> getMyChildTags(@AuthenticationPrincipal String userId) {
        List<ChildTagResponse> response = tagService.getUserChildTags(Long.parseLong(userId));
        return ApiResponse.success(response);
    }

    @GetMapping("/{tagId}/videos")
    public ApiResponse<List<TagVideoResponse>> getVideosByTag(
            @AuthenticationPrincipal String userId,
            @PathVariable("tagId") Long tagId
    ) {
        List<TagVideoResponse> response = tagService.getVideosByTag(tagId, Long.parseLong(userId));
        return ApiResponse.success(response);
    }

    @GetMapping("/parent-with-child")
    public ApiResponse<List<ParentTagWithChildResponse>> getParentTagsWithChild() {
        List<ParentTagWithChildResponse> response = tagService.getParentTagsWithChild();
        return ApiResponse.success(response);
    }
}
