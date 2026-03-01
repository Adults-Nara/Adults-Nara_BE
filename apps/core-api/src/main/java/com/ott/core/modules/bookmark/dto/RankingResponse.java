package com.ott.core.modules.bookmark.dto;
import com.ott.common.persistence.entity.VideoMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RankingResponse {
    private int rank;             // 순위 (1~10)
    private Long videoId;         // 영상 ID
    private String title;         // 영상 제목
    private String thumbnailUrl;  // 썸네일 URL
    private double rankingScore;  // 랭킹 점수

    public static RankingResponse of(int rank, VideoMetadata metadata, Double score) {
        return RankingResponse.builder()
                .rank(rank)
                .videoId(metadata.getVideoId())
                .title(metadata.getTitle())
                .thumbnailUrl(metadata.getThumbnailUrl())
                .rankingScore(score != null ? score : metadata.getBookmarkCount())
                .build();
    }
}
