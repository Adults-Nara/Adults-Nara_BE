package com.ott.core.modules.tag.service;

import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.entity.WatchHistory;
import com.ott.core.modules.tag.dto.response.ChildTagResponse;
import com.ott.core.modules.tag.dto.response.TagVideoResponse;
import com.ott.core.modules.tag.repository.VideoTagRepository;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.usertag.repository.UserTagRepository;
import com.ott.core.modules.watch.repository.WatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final UserTagRepository userTagRepository;
    private final VideoTagRepository videoTagRepository;
    private final UserRepository userRepository;
    private final WatchHistoryRepository watchHistoryRepository;

    public List<ChildTagResponse> getUserChildTags(Long userId) {
        List<Tag> tagList = userTagRepository.findChildTagsByUserId(userId);
        return tagList.stream()
                .map(tag -> new ChildTagResponse(
                        String.valueOf(tag.getId()),
                        tag.getTagName())).toList();
    }

    public List<TagVideoResponse> getVideosByTag(Long tagId, Long userId) {
        List<VideoMetadata> videoMetadataList = videoTagRepository.findTop10VideosByTagId(tagId, PageRequest.of(0, 10));

        List<Long> uploaderIds = videoMetadataList.stream()
                .map(VideoMetadata::getUserId)
                .distinct().toList();

        Map<Long, String> uploaderNameMap = userRepository.findAllById(uploaderIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        List<Long> videoMetadataIds = videoMetadataList.stream()
                .map(VideoMetadata::getId).toList();

        List<WatchHistory> watchHistoryList = watchHistoryRepository.findByUserIdAndVideoMetadataIdIn(userId, videoMetadataIds);
        Map<Long, Integer> watchPositionMap = watchHistoryList.stream()
                .collect(Collectors.toMap(
                        wh -> wh.getVideoMetadata().getId(),
                        WatchHistory::getLastPosition
                ));

        return videoMetadataList.stream()
                .map(vm -> new TagVideoResponse(
                        String.valueOf(vm.getVideoId()),
                        vm.getTitle(),
                        vm.getThumbnailUrl(),
                        vm.getViewCount(),
                        uploaderNameMap.getOrDefault(vm.getUserId(), ""),
                        calculateWatchProgressPercent(watchPositionMap.get(vm.getId()), vm.getDuration()),
                        vm.getCreatedAt(),
                        vm.getDuration()
                )).toList();
    }

    private double calculateWatchProgressPercent(Integer lastPosition, Integer duration) {
        if (duration == null || duration <= 0 || lastPosition == null) return 0.0;
        return Math.min(100.0, (double) lastPosition / duration * 100);
    }

}
