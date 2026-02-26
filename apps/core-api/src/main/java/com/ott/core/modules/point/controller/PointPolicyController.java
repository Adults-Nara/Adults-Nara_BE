package com.ott.core.modules.point.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.point.dto.PointPolicyResponse;
import com.ott.core.modules.point.dto.PointPolicyUpdateRequest;
import com.ott.core.modules.point.service.PointPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/point/policies")
public class PointPolicyController {

    private final PointPolicyService pointPolicyService;

    @GetMapping
    public ApiResponse<List<PointPolicyResponse>> getAllPolicies() {
        return ApiResponse.success(pointPolicyService.getAllPolicies());
    }

    @PutMapping
    public ApiResponse<Void> updatePolicy(@RequestBody PointPolicyUpdateRequest request) {
        pointPolicyService.updatePolicyValue(request.getPolicyName(), request.getPolicyValue());
        return ApiResponse.success(null);
    }
}
