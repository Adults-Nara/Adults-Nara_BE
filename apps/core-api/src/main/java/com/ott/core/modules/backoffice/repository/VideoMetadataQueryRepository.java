package com.ott.core.modules.backoffice.repository;

import com.ott.core.modules.backoffice.dto.AdminContentResponse;
import com.ott.core.modules.backoffice.dto.UploaderContentResponse;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ott.common.persistence.entity.QTag.tag;
import static com.ott.common.persistence.entity.QVideo.*;
import static com.ott.common.persistence.entity.QVideoMetadata.*;
import static com.ott.common.persistence.entity.QVideoTag.*;

@Repository
@RequiredArgsConstructor
public class VideoMetadataQueryRepository {

    private final JPAQueryFactory query;

    public Page<UploaderContentResponse> findUploaderContents(Long userId, String keyword, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(videoMetadata.userId.eq(userId));
        where.and(videoMetadata.deleted.eq(false));

        if (keyword != null && !keyword.isBlank()) {
            String escaped = keyword.replace("%", "\\%").replace("_", "\\_");
            String pattern = "%" + escaped + "%";
            where.and(
                    videoMetadata.title.like(pattern, '\\')
                            .or(videoMetadata.description.like(pattern, '\\'))
                            .or(tag.tagName.like(pattern, '\\'))
            );
        }

        // count 쿼리
        Long total = query
                .select(videoMetadata.id.countDistinct())
                .from(videoMetadata)
                .innerJoin(video).on(video.id.eq(videoMetadata.videoId))
                .leftJoin(videoTag).on(videoTag.videoMetadata.eq(videoMetadata))
                .leftJoin(tag).on(videoTag.tag.eq(tag))
                .where(where)
                .fetchOne();

        if (total == null || total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // data 쿼리
        List<UploaderContentResponse> content = query
                .select(Projections.constructor(UploaderContentResponse.class,
                        videoMetadata.id.stringValue(),
                        videoMetadata.thumbnailUrl,
                        videoMetadata.title,
                        videoMetadata.description,
                        videoMetadata.otherVideoUrl,
                        videoMetadata.viewCount,
                        videoMetadata.likeCount,
                        videoMetadata.dislikeCount,
                        videoMetadata.commentCount,
                        video.visibility,
                        videoMetadata.createdAt
                ))
                .from(videoMetadata)
                .innerJoin(video).on(video.id.eq(videoMetadata.videoId))
                .leftJoin(videoTag).on(videoTag.videoMetadata.eq(videoMetadata))
                .leftJoin(tag).on(videoTag.tag.eq(tag))
                .where(where)
                .groupBy(
                        videoMetadata.id,
                        videoMetadata.thumbnailUrl,
                        videoMetadata.title,
                        videoMetadata.description,
                        videoMetadata.otherVideoUrl,
                        videoMetadata.viewCount,
                        videoMetadata.likeCount,
                        videoMetadata.dislikeCount,
                        videoMetadata.commentCount,
                        video.visibility,
                        videoMetadata.createdAt
                )
                .orderBy(toOrderSpecifier(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    public Page<AdminContentResponse> findAdminContents(String keyword, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(videoMetadata.deleted.eq(false));

        if (keyword != null && !keyword.isBlank()) {
            String escaped = keyword.replace("%", "\\%").replace("_", "\\_");
            String pattern = "%" + escaped + "%";
            where.and(
                    videoMetadata.title.like(pattern, '\\')
                            .or(videoMetadata.description.like(pattern, '\\'))
                            .or(tag.tagName.like(pattern, '\\'))
            );
        }

        Long total = query
                .select(videoMetadata.id.countDistinct())
                .from(videoMetadata)
                .innerJoin(video).on(video.id.eq(videoMetadata.videoId))
                .leftJoin(videoTag).on(videoTag.videoMetadata.eq(videoMetadata))
                .leftJoin(tag).on(videoTag.tag.eq(tag))
                .where(where)
                .fetchOne();

        if (total == null || total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<AdminContentResponse> content = query
                .select(Projections.constructor(AdminContentResponse.class,
                        videoMetadata.id.stringValue(),
                        videoMetadata.thumbnailUrl,
                        videoMetadata.title,
                        videoMetadata.description,
                        videoMetadata.otherVideoUrl,
                        videoMetadata.viewCount,
                        videoMetadata.likeCount,
                        videoMetadata.dislikeCount,
                        videoMetadata.commentCount,
                        video.visibility,
                        videoMetadata.createdAt
                ))
                .from(videoMetadata)
                .innerJoin(video).on(video.id.eq(videoMetadata.videoId))
                .leftJoin(videoTag).on(videoTag.videoMetadata.eq(videoMetadata))
                .leftJoin(tag).on(videoTag.tag.eq(tag))
                .where(where)
                .groupBy(
                        videoMetadata.id,
                        videoMetadata.thumbnailUrl,
                        videoMetadata.title,
                        videoMetadata.description,
                        videoMetadata.otherVideoUrl,
                        videoMetadata.viewCount,
                        videoMetadata.likeCount,
                        videoMetadata.dislikeCount,
                        videoMetadata.commentCount,
                        video.visibility,
                        videoMetadata.createdAt
                )
                .orderBy(toOrderSpecifier(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    private OrderSpecifier<?> toOrderSpecifier(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return videoMetadata.createdAt.desc();
        }
        Sort.Order order = pageable.getSort().iterator().next();
        boolean asc = order.isAscending();

        return switch (order.getProperty()) {
            case "createdAt" -> asc ? videoMetadata.createdAt.asc() : videoMetadata.createdAt.desc();
            case "title" -> asc ? videoMetadata.title.asc() : videoMetadata.title.desc();
            case "viewCount" -> asc ? videoMetadata.viewCount.asc() : videoMetadata.viewCount.desc();
            default -> videoMetadata.createdAt.desc();
        };
    }

}
