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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "Video API", description = "비디오 업로드/재생 API")
public class VideoController {
    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @Operation(
            summary = "Multipart 업로드 초기화",
            description = "S3 Multipart Upload를 시작하고, uploadId/videoId/Presigned URL 발급 등 필요한 정보를 생성합니다."
    )
    @PostMapping("/api/v1/videos/upload/multipart/init")
    public ApiResponse<MultipartInitResponse> initMultipartUpload(@AuthenticationPrincipal String userId,
                                                                  @RequestBody MultipartInitRequest request) {
        MultipartInitResult result = videoService.initMultipartUpload(Long.parseLong(userId), request.contentType(), request.sizeBytes());
        return ApiResponse.success(MultipartInitResponse.of(result));
    }

    @Operation(
            summary = "Multipart 업로드 완료",
            description = "클라이언트가 업로드한 parts 정보를 바탕으로 Multipart Upload를 Complete 합니다."
    )
    @PostMapping("/api/v1/videos/{videoId}/upload/multipart/complete")
    public ApiResponse<?> completeMultipartUpload(@PathVariable Long videoId,
                                                  @AuthenticationPrincipal String userId,
                                                  @RequestBody MultipartCompleteRequest request) {
        videoService.completeMultipartUpload(videoId, Long.parseLong(userId), request.uploadId(), request.parts(), request.sizeBytes());
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "Multipart 업로드 중단(Abort)",
            description = "업로드를 중단하고 MultipartUploadId 기준으로 abort 처리합니다."
    )
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
    @Operation(
            summary = "비디오 업로드 (썸네일 + 메타데이터)",
            description = "multipart/form-data로 image(파일)과 data(JSON)를 함께 전송합니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = VideoUploadMultipart.class),
                    // data 파트를 JSON으로 인코딩한다고 swagger에 알려줌 (중요)
                    encoding = {
                            @Encoding(name = "data", contentType = MediaType.APPLICATION_JSON_VALUE)
                    }
            )
    )
    @PostMapping(value = "/api/v1/videos/{videoId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> upload(@PathVariable Long videoId,
                                 @AuthenticationPrincipal String userId,
                                 @RequestPart("image") MultipartFile image,
                                 @RequestPart("data") UploadRequest request) {
        videoService.upload(videoId, Long.parseLong(userId), image, request.title(), request.description(),
                request.videoType(), request.otherVideoUrl(), request.tagIds(), request.visibility());
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "비디오 재생",
            description = "재생에 필요한 정보(m3u8 URL)를 반환합니다. 응답 헤더에 쿠키를 함께 내려줍니다."
    )
    @GetMapping("/api/v1/videos/{videoId}/play")
    public ResponseEntity<ApiResponse<PlayResponse>> play(@PathVariable Long videoId) {
        PlayResult result = videoService.play(videoId);
        return ResponseEntity.ok()
                .headers(result.httpHeaders())
                .body(ApiResponse.success(PlayResponse.of(result)));
    }

    /**
     * Swagger 문서용 multipart 스키마
     */
    class VideoUploadMultipart {

        @Schema(type = "string", format = "binary", description = "썸네일 이미지 파일")
        public MultipartFile image;

        @Schema(description = "업로드 메타데이터(JSON)")
        public UploadRequest data;
    }
}
