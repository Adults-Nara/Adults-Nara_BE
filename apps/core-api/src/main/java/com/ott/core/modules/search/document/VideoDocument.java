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
    private Long videoId; // 검색 결과 클릭 시 비디오로 바로가기 위해 videoId를 ES의 ID로 사용

    @Field(type = FieldType.Long)
    private Long metadataId;

    @Field(type = FieldType.Long)
    private Long userId;

    // Nori 형태소 분석기가 적용된 검색용 필드
    @Field(type = FieldType.Text, analyzer = "korean_nori_analyzer")
    private String title;

    @Field(type = FieldType.Text, analyzer = "korean_nori_analyzer")
    private String description;

    // 필터링용 필드 (Keyword 타입으로 정확도 일치 시 캐싱 활용)
    @Field(type = FieldType.Keyword)
    private VideoType videoType;

    @Field(type = FieldType.Keyword)
    private List<String> tags; // VideoTag 테이블의 태그 이름들을 합쳐서 저장 (빠른 필터링용)

    // 정렬(Sorting) 및 가중치 계산용 필드
    @Field(type = FieldType.Integer)
    private int viewCount;

    @Field(type = FieldType.Integer)
    private int likeCount;

    @Field(type = FieldType.Boolean)
    private boolean deleted; // 소프트 딜리트 필터링용

    // 검색 조건으로는 안 쓰이지만, 검색 결과 리스트 UI를 그리기 위해 반환해 줄 데이터 (index = false로 메모리 최적화)
    @Field(type = FieldType.Keyword, index = false)
    private String thumbnailUrl;

    @Field(type = FieldType.Integer, index = false)
    private Integer duration;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private OffsetDateTime createdAt;
}