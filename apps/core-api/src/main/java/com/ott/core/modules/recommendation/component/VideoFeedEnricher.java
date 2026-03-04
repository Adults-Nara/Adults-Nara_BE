package com.ott.core.modules.recommendation.component;

import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.WatchHistory;
import com.ott.core.modules.recommendation.dto.VideoFeedResponseDto;
import com.ott.core.modules.search.document.VideoDocument;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.watch.repository.WatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VideoFeedEnricher {

    private final UserRepository userRepository;
    private final WatchHistoryRepository watchHistoryRepository;

    /**
     * Elasticsearch Document 목록을 기반으로 DB 데이터를 조합하여 최종 DTO로 변환
     */
    public List<VideoFeedResponseDto> enrich(List<VideoDocument> documents, Long currentUserId) {
        if (documents.isEmpty()) return List.of();

        // 1. 업로더(User) 정보 일괄 조회
        Set<Long> uploaderIds = documents.stream()
                .map(VideoDocument::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> uploaderMap = userRepository.findAllById(uploaderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 2. 시청 기록(WatchHistory) 일괄 조회
        List<Long> metadataIds = documents.stream()
                .map(VideoDocument::getMetadataId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, WatchHistory> historyMap = new HashMap<>();
        if (currentUserId != null && !metadataIds.isEmpty()) {
            List<WatchHistory> histories = watchHistoryRepository.findByUserIdAndVideoMetadataIdIn(currentUserId, metadataIds);
            for (WatchHistory h : histories) {
                historyMap.put(h.getVideoMetadata().getId(), h);
            }
        }

        // 3. 최종 DTO 조립
        return documents.stream().map(doc -> {
            User uploader = uploaderMap.get(doc.getUserId());
            String nickname = (uploader != null) ? uploader.getNickname() : "탈퇴한 사용자";
            String profileUrl = (uploader != null) ? uploader.getProfileImageUrl() : null;

            int progress = 0;
            WatchHistory history = historyMap.get(doc.getMetadataId());
            if (history != null && doc.getDuration() != null && doc.getDuration() > 0 && history.getLastPosition() != null) {
                progress = (int) Math.min(100.0, Math.round(((double) history.getLastPosition() / doc.getDuration()) * 100));
            }

            return VideoFeedResponseDto.of(doc, nickname, profileUrl, progress);
        }).toList();
    }
}