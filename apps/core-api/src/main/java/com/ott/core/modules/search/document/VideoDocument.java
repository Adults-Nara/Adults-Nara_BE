package com.ott.core.modules.search.document;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Getter
@Builder
@Document(indexName = "videos")
public class VideoDocument {
    @Id // ES의 문서 ID (DB의 video_metadata_id와 동일하게 맞춤)
    private Long id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Integer)
    private Integer viewCount;

    @Field(type = FieldType.Integer)
    private Integer likeCount;

    @Field(type = FieldType.Keyword)
    private String createdAt;

    @Field(type = FieldType.Keyword)
    private String videoType;

    public static VideoDocument of(com.ott.common.persistence.entity.VideoMetadata video, List<String> tags) {
        return VideoDocument.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .tags(tags)
                .viewCount(video.getViewCount())
                .likeCount(video.getLikeCount())
                .createdAt(video.getCreatedAt().toString())
                .videoType(video.getVideoType() != null ? video.getVideoType().name() : "NONE")
                .build();
    }
}
