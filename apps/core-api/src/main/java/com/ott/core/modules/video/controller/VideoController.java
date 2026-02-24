package com.ott.core.modules.video.controller;

import com.ott.common.response.ApiResponse;
import com.ott.core.modules.video.controller.request.MultipartCompleteRequest;
import com.ott.core.modules.video.controller.request.MultipartInitRequest;
import com.ott.core.modules.video.controller.request.UploadRequest;
import com.ott.core.modules.video.controller.response.MultipartInitResponse;
import com.ott.core.modules.video.controller.response.PlayResponse;
import com.ott.core.modules.video.dto.PlayResult;
import com.ott.core.modules.video.dto.multipart.MultipartInitResult;
import com.ott.core.modules.video.service.VideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class VideoController {
    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping("/api/v1/videos/upload/multipart/init")
    public ApiResponse<MultipartInitResponse> initMultipartUpload(@AuthenticationPrincipal String userId,
                                                                  @RequestBody MultipartInitRequest request) {
        MultipartInitResult result = videoService.initMultipartUpload(Long.parseLong(userId), request.contentType(), request.sizeBytes());
        return ApiResponse.success(MultipartInitResponse.of(result));
    }

    @PostMapping("/api/v1/videos/{videoId}/upload/multipart/complete")
    public ApiResponse<?> completeMultipartUpload(@PathVariable Long videoId,
                                                  @AuthenticationPrincipal String userId,
                                                  @RequestBody MultipartCompleteRequest request) {
        videoService.completeMultipartUpload(videoId, Long.parseLong(userId), request.uploadId(), request.parts(), request.sizeBytes());
        return ApiResponse.success(null);
    }

    @PostMapping("/api/v1/videos/{videoId}/upload/multipart/abort")
    public ApiResponse<?> abortMultipartUpload(@PathVariable Long videoId,
                                               @AuthenticationPrincipal String userId,
                                               @RequestParam String uploadId) {
        videoService.abortMultipartUpload(videoId, Long.parseLong(userId), uploadId);
        return ApiResponse.success();
    }

    /*
        curl -X POST "http://localhost:8080/api/v1/videos/{videoId}/upload" \
          -H "Content-Type: multipart/form-data" \
          -F "image=@thumbnail.jpg" \
          -F 'data={"title":"my title","description":"my description"};type=application/json'
     */
    @PostMapping("/api/v1/videos/{videoId}/upload")
    public ApiResponse<?> upload(@PathVariable Long videoId,
                                 @AuthenticationPrincipal String userId,
                                 @RequestPart("image") MultipartFile image,
                                 @RequestPart("data") UploadRequest request) {
        videoService.upload(videoId, Long.parseLong(userId), image, request.title(), request.description(),
                request.videoType(), request.otherVideoUrl(), request.tagIds(), request.visibility());
        return ApiResponse.success(null);
    }

    @GetMapping("/api/v1/videos/{videoId}/play")
    public ResponseEntity<ApiResponse<PlayResponse>> play(@PathVariable Long videoId) {
        PlayResult result = videoService.play(videoId);
        return ResponseEntity.ok()
                .headers(result.httpHeaders())
                .body(ApiResponse.success(PlayResponse.of(result)));
    }

}
