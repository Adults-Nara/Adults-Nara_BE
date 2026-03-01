package com.ott.core.modules.search.document;

import com.ott.common.persistence.enums.VideoType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.OffsetDateTime;
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

    @Field(type = FieldType.Keyword)
    private VideoType videoType;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    // 정렬(Sorting) 및 가중치 계산용 필드
    @Field(type = FieldType.Integer)
    private int viewCount;

    @Field(type = FieldType.Integer)
    private int likeCount;

    @Field(type = FieldType.Boolean)
    private boolean deleted;

    @Field(type = FieldType.Keyword, index = false)
    private String thumbnailUrl;

    @Field(type = FieldType.Integer, index = false)
    private Integer duration;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private OffsetDateTime createdAt;
}