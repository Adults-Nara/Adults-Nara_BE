package com.ott.core.modules.backoffice.repository;

import com.ott.common.persistence.entity.Tag;
import com.ott.common.persistence.entity.Video;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.entity.VideoTag;
import com.ott.common.persistence.enums.Visibility;
import com.ott.core.modules.backoffice.dto.UploaderContentResponse;
import com.ott.core.modules.video.service.SignedCookieProcessor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class VideoMetadataQueryRepositoryTest {

    @MockitoBean private SignedCookieProcessor signedCookieProcessor;
    @MockitoBean private software.amazon.awssdk.services.s3.S3Client s3Client;
    @MockitoBean private software.amazon.awssdk.services.s3.presigner.S3Presigner s3Presigner;

    @Autowired EntityManager em;
    @Autowired VideoMetadataQueryRepository videoMetadataQueryRepository;

    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 200L;

    @BeforeEach
    void setUp() {
        Video video1 = new Video(1L, null);
        video1.setVisibility(Visibility.PUBLIC);

        Video video2 = new Video(2L, null);

        Video video3 = new Video(3L, null);
        video3.setVisibility(Visibility.PUBLIC);

        Video video4 = new Video(4L, null);
        video4.setVisibility(Visibility.PUBLIC);

        em.persist(video1);
        em.persist(video2);
        em.persist(video3);
        em.persist(video4);

        VideoMetadata videoMetadata1 = VideoMetadata.builder()
                .id(1L).videoId(1L).userId(USER_ID)
                .title("자바 강의").description("스프링 부트 입문").deleted(false).build();

        VideoMetadata videoMetadata2 = VideoMetadata.builder()
                .id(2L).videoId(2L).userId(USER_ID)
                .title("리액트 강의").description("프론트엔드 입문").deleted(false).build();

        VideoMetadata videoMetadata3 = VideoMetadata.builder()
                .id(3L).videoId(3L).userId(USER_ID)
                .title("삭제된 강의").description("삭제됨").deleted(true).build();

        VideoMetadata videoMetadata4 = VideoMetadata.builder()
                .id(4L).videoId(4L).userId(OTHER_USER_ID)
                .title("다른 유저 강의").description("다른 유저").deleted(false).build();
        em.persist(videoMetadata1);
        em.persist(videoMetadata2);
        em.persist(videoMetadata3);
        em.persist(videoMetadata4);

        Tag tag1 = new Tag("스프링");
        em.persist(tag1);

        VideoTag videoTag1 = new VideoTag(videoMetadata1, tag1);
        ReflectionTestUtils.setField(videoTag1, "id", 1L);
        em.persist(videoTag1);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("userId로 본인 컨텐츠만 조회")
    void 본인_컨텐츠만_조회() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<UploaderContentResponse> result = videoMetadataQueryRepository.findUploaderContents(USER_ID, null, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(UploaderContentResponse::title)
                .containsExactlyInAnyOrder("자바 강의", "리액트 강의");
    }

    @Test
    @DisplayName("삭제된 컨텐츠는 조회되지 않음")
    void 삭제된_컨텐츠_제외() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<UploaderContentResponse> result = videoMetadataQueryRepository.findUploaderContents(USER_ID, null, pageable);

        assertThat(result.getContent())
                .extracting(UploaderContentResponse::title)
                .doesNotContain("삭제된 강의");
    }

    @Test
    @DisplayName("title 키워드로 검색")
    void title_키워드_검색() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<UploaderContentResponse> result = videoMetadataQueryRepository.findUploaderContents(USER_ID, "자바", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("자바 강의");
    }

    @Test
    @DisplayName("tag 키워드로 검색")
    void tag_키워드_검색() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<UploaderContentResponse> result = videoMetadataQueryRepository.findUploaderContents(USER_ID, "스프링", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("자바 강의");
    }

    @Test
    @DisplayName("페이징 테스트")
    void 페이징_테스트() {
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<UploaderContentResponse> result = videoMetadataQueryRepository.findUploaderContents(USER_ID, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("검색 결과가 없을 때 빈 페이지 반환")
    void 검색_결과없음_빈페이지() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<UploaderContentResponse> result = videoMetadataQueryRepository.findUploaderContents(USER_ID, "존재하지않는 키워드", pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}