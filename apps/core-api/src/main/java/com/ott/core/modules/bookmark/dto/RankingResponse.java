package com.ott.core.modules.bookmark.dto;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.enums.VideoType;
import lombok.Builder;


@Builder
public record RankingResponse (
    int rank,
    double rankingScore,
    String videoId,
    String thumbnailSrc,
    String title,
    String uploader,
    String uploaderProfileImageUrl,
    int progress,
    Integer duration,
    int views,
    String date,
    VideoType videoType

) {
    public static RankingResponse of ( int rank, VideoMetadata metadata, Double score, String uploader, String
        profileUrl,int progress){
        return RankingResponse.builder()
                .rank(rank)
                .rankingScore(score != null ? score : metadata.getBookmarkCount())
                .videoId(String.valueOf(metadata.getVideoId()))
                .thumbnailSrc(metadata.getThumbnailUrl())
                .title(metadata.getTitle())
                .uploader(uploader)
                .uploaderProfileImageUrl(profileUrl)
                .progress(progress)
                .duration(metadata.getDuration() != null ? metadata.getDuration() : 0)
                .views(metadata.getViewCount())
                .date(metadata.getCreatedAt() != null ? metadata.getCreatedAt().toString() : null)
                .videoType(metadata.getVideoType())
                .build();
    }
}
