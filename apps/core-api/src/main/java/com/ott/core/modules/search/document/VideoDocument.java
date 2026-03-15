package com.ott.core.modules.search.document;

import com.ott.common.persistence.entity.VideoAiAnalysis;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.enums.VideoType;
import com.ott.core.modules.search.util.ChosungUtils;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(indexName = "video_search", createIndex = false)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VideoDocument {

    @Id
    private Long videoId;

    @Field(type = FieldType.Long)
    private Long metadataId;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Text, analyzer = "korean_nori_analyzer")
    private String title;

    @Field(type = FieldType.Text, analyzer = "korean_nori_analyzer")
    private String description;

    @Field(type = FieldType.Text, analyzer = "korean_nori_analyzer")
    private String summary;

    @Field(type = FieldType.Keyword)
    private String titleChosung;

    @Field(type = FieldType.Keyword)
    private VideoType videoType;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Dense_Vector, dims = 384, similarity = "cosine", index = true)
    private List<Float> embedding;

    @Field(type = FieldType.Integer)
    private int viewCount;

    @Field(type = FieldType.Integer)
    private int likeCount;

    @Field(type = FieldType.Boolean)
    private boolean deleted;

    @Field(type = FieldType.Boolean)
    private boolean isAd;

    @Field(type = FieldType.Keyword, index = false)
    private String thumbnailUrl;

    @Field(type = FieldType.Integer, index = false)
    private Integer duration;

    @Field(type = FieldType.Date, format = DateFormat.date_time, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime createdAt;

    public static VideoDocument from(VideoMetadata metadata, List<String> tagNames, VideoAiAnalysis aiAnalysis) {
        List<Float> embeddingList = null;
        if (aiAnalysis != null && aiAnalysis.getEmbedding() != null) {
            embeddingList = new ArrayList<>(aiAnalysis.getEmbedding().length);
            for (float v : aiAnalysis.getEmbedding()) {
                embeddingList.add(v);
            }
        }
        return VideoDocument.builder()
                .videoId(metadata.getVideoId())
                .metadataId(metadata.getId())
                .userId(metadata.getUserId())
                .title(metadata.getTitle())
                .titleChosung(ChosungUtils.extract(metadata.getTitle()))
                .description(metadata.getDescription())
                .videoType(metadata.getVideoType())
                .tags(tagNames)
                .summary(aiAnalysis != null ? aiAnalysis.getSummary() : null)
                .embedding(embeddingList)
                .viewCount(metadata.getViewCount())
                .likeCount(metadata.getLikeCount())
                .deleted(metadata.isDeleted())
                .isAd(metadata.isAd())
                .thumbnailUrl(metadata.getThumbnailUrl())
                .duration(metadata.getDuration())
                .createdAt(metadata.getCreatedAt())
                .build();
    }
}
