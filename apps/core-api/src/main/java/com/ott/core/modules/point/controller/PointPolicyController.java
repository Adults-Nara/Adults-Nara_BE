package com.ott.core.modules.point.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.point.dto.PointPolicyResponse;
import com.ott.core.modules.point.dto.PointPolicyUpdateRequest;
import com.ott.core.modules.point.service.PointPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "포인트 정책 관리 API (Admin)", description = "포인트 정책 조회 및 수정 API")

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/point/policies")
public class PointPolicyController {

    private final PointPolicyService pointPolicyService;

    @Operation(summary = "전체 포인트 정책 조회", description = "시스템에 등록된 모든 포인트 정책의 이름과 값을 조회합니다.")
    @GetMapping
    public ApiResponse<List<PointPolicyResponse>> getAllPolicies() {
        return ApiResponse.success(pointPolicyService.getAllPolicies());
    }

    @Operation(summary = "포인트 정책 수정", description = "특정 포인트 정책(예: 리뷰 작성 포인트 등)의 부여량을 수정합니다.")
    @PutMapping
    public ApiResponse<Void> updatePolicy(@RequestBody PointPolicyUpdateRequest request) {
        pointPolicyService.updatePolicyValue(request.getPolicyName(), request.getPolicyValue());
        return ApiResponse.success(null);
    }
}
