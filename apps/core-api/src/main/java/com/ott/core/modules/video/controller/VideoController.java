package com.ott.core.modules.video.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.video.controller.request.MultipartCompleteRequest;
import com.ott.core.modules.video.controller.request.MultipartInitRequest;
import com.ott.core.modules.video.controller.response.MultipartInitResponse;
import com.ott.core.modules.video.controller.response.PlayResponse;
import com.ott.core.modules.video.dto.PlayResult;
import com.ott.core.modules.video.dto.multipart.MultipartInitResult;
import com.ott.core.modules.video.service.VideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class VideoController {
    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping("/api/v1/videos/upload/multipart/init")
    public ApiResponse<MultipartInitResponse> initMultipartUpload(@RequestBody MultipartInitRequest request) {
        MultipartInitResult result = videoService.initMultipartUpload(request.contentType(), request.sizeBytes());
        return ApiResponse.success(MultipartInitResponse.of(result));
    }

    @PostMapping("/api/v1/videos/{videoId}/upload/multipart/complete")
    public ApiResponse<?> completeMultipartUpload(@PathVariable Long videoId,
                                                     @RequestBody MultipartCompleteRequest request) {
        videoService.completeMultipartUpload(videoId, request.uploadId(), request.parts(), request.sizeBytes());
        return ApiResponse.success(null);
    }

    @PostMapping("/api/v1/videos/{videoId}/upload/multipart/abort")
    public ApiResponse<?> abortMultipartUpload(@PathVariable Long videoId,
                                                  @RequestParam String uploadId) {
        videoService.abortMultipartUpload(videoId, uploadId);
        return ApiResponse.success();
    }

    @GetMapping("/api/v1/videos/{videoId}/play")
    public ResponseEntity<ApiResponse<PlayResponse>> play(@PathVariable Long videoId) {
        PlayResult result = videoService.play(videoId);
        return ResponseEntity.ok()
                .headers(result.httpHeaders())
                .body(ApiResponse.success(PlayResponse.of(result)));
    }

}
