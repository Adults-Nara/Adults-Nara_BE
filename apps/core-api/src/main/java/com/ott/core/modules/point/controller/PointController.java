package com.ott.core.modules.point.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.point.dto.PointTransactionHistoryDTO;
import com.ott.core.modules.point.dto.PointTransactionHistoryRequest;
import com.ott.core.modules.point.dto.PointTransactionHistoryResponse;
import com.ott.core.modules.point.dto.UserPointBalanceResponse;
import com.ott.core.modules.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/point")
public class PointController {
    private final PointService pointService;

    @GetMapping
    public ApiResponse<UserPointBalanceResponse> getMyPointBalance(Long userId) {
        return ApiResponse.success(pointService.findUserCurrentPoint(userId));
    }

    @GetMapping("/details")
    public ApiResponse<List<PointTransactionHistoryResponse>> getMyPointTransactionHistory(Long userId, PointTransactionHistoryRequest req) {
        return ApiResponse.success(pointService.findUserPointHistory(userId, req));
    }

}
