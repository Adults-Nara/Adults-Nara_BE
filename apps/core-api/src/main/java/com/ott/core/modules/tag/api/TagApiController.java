package com.ott.core.modules.tag.api;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.tag.dto.response.ChildTagResponse;
import com.ott.core.modules.tag.dto.response.ParentTagWithChildResponse;
import com.ott.core.modules.tag.dto.response.TagVideoResponse;
import com.ott.core.modules.tag.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "태그 API", description = "태그 조회 및 태그별 영상 조회 API")
public class TagApiController {

    private final TagService tagService;

    @Operation(summary = "내 관심 태그(자식) 목록 조회", description = "로그인한 사용자가 설정한 관심 자식 태그 목록을 반환합니다.")
    @GetMapping("/my-child-tags")
    public ApiResponse<List<ChildTagResponse>> getMyChildTags(@AuthenticationPrincipal String userId) {
        List<ChildTagResponse> response = tagService.getUserChildTags(Long.parseLong(userId));
        return ApiResponse.success(response);
    }

    @Operation(summary = "태그별 영상 목록 조회", description = "특정 태그에 속한 영상 목록을 반환합니다.")
    @GetMapping("/{tagId}/videos")
    public ApiResponse<List<TagVideoResponse>> getVideosByTag(
            @AuthenticationPrincipal String userId,
            @PathVariable("tagId") Long tagId
    ) {
        List<TagVideoResponse> response = tagService.getVideosByTag(tagId, Long.parseLong(userId));
        return ApiResponse.success(response);
    }

    @Operation(summary = "부모 태그 + 자식 태그 목록 조회", description = "부모 태그와 그에 속한 자식 태그를 함께 반환합니다.")
    @GetMapping("/parent-with-child")
    public ApiResponse<List<ParentTagWithChildResponse>> getParentTagsWithChild() {
        List<ParentTagWithChildResponse> response = tagService.getParentTagsWithChild();
        return ApiResponse.success(response);
    }
}
