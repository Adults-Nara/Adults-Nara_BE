package com.ott.core.modules.video.service;


import com.ott.core.modules.video.dto.multipart.CompletedPartDto;
import com.ott.core.modules.video.dto.multipart.MultipartInitResult;

import java.util.List;

public interface PresignedMultipartProcessor {

    MultipartInitResult initMultipart(Long videoId, String bucket, String objectKey, String contentType, long sizeBytes);

    void completeMultipart(String bucket, String objectKey, String uploadId, List<CompletedPartDto> parts);

    void abortMultipart(String bucket, String objectKey, String uploadId);
}
