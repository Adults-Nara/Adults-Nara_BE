package com.ott.core.modules.backoffice.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.Video;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.entity.VideoTag;
import com.ott.common.persistence.enums.BanStatus;
import com.ott.common.persistence.enums.UserRole;
import com.ott.core.modules.backoffice.dto.*;
import com.ott.core.modules.backoffice.repository.UserQueryRepository;
import com.ott.core.modules.tag.repository.TagRepository;
import com.ott.core.modules.backoffice.repository.VideoMetadataQueryRepository;
import com.ott.core.modules.tag.repository.VideoTagRepository;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import com.ott.core.modules.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BackofficeService {

    private final VideoMetadataQueryRepository videoMetadataQueryRepository;
    private final VideoMetadataRepository videoMetadataRepository;
    private final VideoRepository videoRepository;
    private final VideoTagRepository videoTagRepository;
    private final TagRepository tagRepository;
    private final UserQueryRepository userQueryRepository;
    private final UserRepository userRepository;

    public Page<UploaderContentResponse> getUploaderContents(Long userId, String keyword, Pageable pageable) {
        return videoMetadataQueryRepository.findUploaderContents(userId, keyword, pageable);
    }

    public Page<AdminContentResponse> getAdminContents(String keyword, Pageable pageable) {
        return videoMetadataQueryRepository.findAdminContents(keyword, pageable);
    }


    @Transactional
    public ContentUpdateResponse updateContent(long userId, Long videoId, ContentUpdateRequest request) {
        VideoMetadata videoMetadata = videoMetadataRepository.findByVideoIdAndDeleted(videoId, false).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!videoMetadata.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (request.title() != null) videoMetadata.setTitle(request.title());
        if (request.description() != null) videoMetadata.setDescription(request.description());
        if (request.thumbnailUrl() != null) videoMetadata.setThumbnailUrl(request.thumbnailUrl());
        if (request.visibility() != null) {
            Video video = videoRepository.findById(videoId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
            video.setVisibility(request.visibility());
        }
        if (request.tagIds() != null) {
            videoTagRepository.deleteAllByVideoMetadata(videoMetadata);

            List<Tag> tags = tagRepository.findAllById(request.tagIds());
            List<VideoTag> videoTags = tags.stream()
                    .map(tag -> new VideoTag(videoMetadata, tag))
                    .toList();
            videoTagRepository.saveAll(videoTags);
        }

        return new ContentUpdateResponse(String.valueOf(videoId));
    }

    @Transactional
    public void deleteContent(long userId, boolean isAdmin, ContentDeleteRequest request) {
        if (request.videoIds() == null || request.videoIds().isEmpty()) {
            return;
        }

        if (isAdmin) {
            videoMetadataRepository.softDeleteByAdmin(request.videoIds());
        } else {
            videoMetadataRepository.softDeleteByUploader(request.videoIds(), userId);
        }
    }

    public Page<AdminUserResponse> getAllUsers(UserRole userRole, String keyword, Pageable pageable) {
        return userQueryRepository.findAllUsers(userRole, keyword, pageable);
    }

    @Transactional
    public UserStatusUpdateResponse updateUserStatus(UserStatusUpdateRequest request) {
        if (request.userIds() == null || request.userIds().isEmpty()) {
            return new UserStatusUpdateResponse(List.of());
        }

        if (request.banStatus() == null || request.banStatus() == BanStatus.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        boolean isActive = request.banStatus() == BanStatus.ACTIVE;
        OffsetDateTime bannedUtil = request.banStatus().isSuspended() ? OffsetDateTime.now().plusDays(request.banStatus().getDays()) : null;
        OffsetDateTime bannedAt = isActive ? null : OffsetDateTime.now();
        String banReason = isActive ? null : request.banReason();

        userRepository.updateBanStatus(request.banStatus(), banReason, bannedUtil, bannedAt, request.userIds());
        return new UserStatusUpdateResponse(request.userIds().stream().map(String::valueOf).toList());
    }

    @Transactional
    public void deleteUser(DeleteUserRequest request) {
        if (request.userIds() == null || request.userIds().isEmpty()) {
            return;
        }
        userRepository.softDeleteUserByAdmin(request.userIds(),BanStatus.DELETED, OffsetDateTime.now());
    }

    public ContentDetailResponse getContentDetail(long userId, Long videoId) {
        VideoMetadata videoMetadata = videoMetadataRepository.findByVideoIdAndDeleted(videoId, false).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!videoMetadata.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Video video = videoRepository.findById(videoId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        List<Tag> tagList = videoTagRepository.findTagsByVideoMetadataId(videoMetadata.getId());
        List<String> tagIds = tagList.stream().map(tag -> String.valueOf(tag.getId())).toList();

        return new ContentDetailResponse(
                String.valueOf(videoId),
                videoMetadata.getTitle(),
                videoMetadata.getDescription(),
                videoMetadata.getThumbnailUrl(),
                video.getVisibility(),
                tagIds,
                videoMetadata.getCreatedAt(),
                videoMetadata.getOtherVideoUrl()
        );
    }
}
