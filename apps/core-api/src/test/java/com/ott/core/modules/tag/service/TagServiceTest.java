package com.ott.core.modules.tag.service;

import com.ott.common.persistence.entity.*;
import com.ott.common.persistence.enums.UserRole;
import com.ott.common.util.IdGenerator;
import com.ott.core.modules.preference.repository.UserPreferenceRepository;
import com.ott.core.modules.tag.dto.response.ChildTagResponse;
import com.ott.core.modules.tag.dto.response.TagVideoResponse;
import com.ott.core.modules.tag.repository.TagRepository;
import com.ott.core.modules.tag.repository.VideoTagRepository;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import com.ott.core.modules.video.service.SignedCookieProcessor;
import com.ott.core.modules.watch.repository.WatchHistoryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TagServiceTest {

    @MockitoBean private SignedCookieProcessor signedCookieProcessor;
    @MockitoBean private software.amazon.awssdk.services.s3.S3Client s3Client;
    @MockitoBean private software.amazon.awssdk.services.s3.presigner.S3Presigner s3Presigner;

    @Autowired private EntityManager em;
    @Autowired private UserRepository userRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private VideoMetadataRepository videoMetadataRepository;
    @Autowired private UserPreferenceRepository userPreferenceRepository;
    @Autowired private TagService tagService;
    @Autowired private VideoTagRepository videoTagRepository;
    @Autowired private WatchHistoryRepository watchHistoryRepository;

    private User saveUser(String email, String nickname) {
        return userRepository.save(new User(email, nickname, "pw", UserRole.VIEWER));
    }

    private Tag saveParentTag(String name) {
        Tag tag = new Tag(name);
        tag.setDepth(0);
        return tagRepository.save(tag);
    }

    private Tag saveChildTag(String name, Tag parent) {
        Tag tag = new Tag(name);
        tag.setParent(parent);
        tag.setDepth(1);
        return tagRepository.save(tag);
    }

    private VideoMetadata saveVideo(Long uploaderId, String title, int viewCount, int duration) {
        VideoMetadata vm = VideoMetadata.builder()
                .videoId(IdGenerator.generate())
                .userId(uploaderId)
                .title(title)
                .thumbnailUrl("https://thumb.example.com/img.jpg")
                .viewCount(viewCount)
                .duration(duration)
                .deleted(false)
                .build();
        return videoMetadataRepository.save(vm);
    }

    @Test
    @DisplayName("사용자와 자식 태그 목록을 점수 내림차순으로 반환")
    void 자식태그_정상반환() {
        User user = saveUser("user@test.com", "유저");
        Tag parentTag = saveParentTag("기술");
        Tag javaTag = saveChildTag("자바", parentTag);
        Tag springTag = saveChildTag("스프링", parentTag);

        userPreferenceRepository.save(new UserPreference(user, javaTag, 5.0));
        userPreferenceRepository.save(new UserPreference(user, springTag, 10.0));

        em.flush();
        em.clear();

        List<ChildTagResponse> result = tagService.getUserChildTags(user.getId());
        assertThat(result).hasSize(2);
        assertThat(result.getFirst().tagName()).isEqualTo("스프링");
        assertThat(result.getLast().tagName()).isEqualTo("자바");
    }

    @Test
    @DisplayName("부모 태그는 결과에 포함되지 않음")
    void 부모태그_반환안됨() {
        User user = saveUser("user@test.com", "유저");
        Tag parentTag = saveParentTag("기술");
        Tag javaTag = saveChildTag("자바", parentTag);

        userPreferenceRepository.save(new UserPreference(user, parentTag, 20.0));
        userPreferenceRepository.save(new UserPreference(user, javaTag, 10.0));

        em.flush();
        em.clear();

        List<ChildTagResponse> result = tagService.getUserChildTags(user.getId());
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().tagName()).isEqualTo("자바");
    }

    @Test
    @DisplayName("태그별 영상 목록과 업로더명, 시청 진행률을 반환")
    void 태그별_영상목록_정상반환() {
        User uploader = saveUser("uploader@test.com", "업로더");
        User viewer = saveUser("viewer@test.com", "시청자");
        Tag parentTag = saveParentTag("기술");
        Tag javaTag = saveChildTag("자바", parentTag);
        VideoMetadata vm = saveVideo(uploader.getId(), "자바 강의", 1000, 300);
        videoTagRepository.save(new VideoTag(vm, javaTag));
        watchHistoryRepository.save(new WatchHistory(viewer, vm, 150));

        em.flush();
        em.clear();

        List<TagVideoResponse> result = tagService.getVideosByTag(javaTag.getId(), viewer.getId());

        assertThat(result).hasSize(1);
        TagVideoResponse response = result.getFirst();
        assertThat(response.title()).isEqualTo("자바 강의");
        assertThat(response.uploaderName()).isEqualTo("업로더");
        assertThat(response.viewCount()).isEqualTo(1000);
        assertThat(response.watchProgressPercent()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("시청 이력이 없으면 진행률 0을 반환")
    void 시청이력_없으면_진행률_0반환() {
        User uploader = saveUser("uploader@test.com", "업로더");
        User viewer = saveUser("viewer@test.com", "시청자");
        Tag parentTag = saveParentTag("기술");
        Tag javaTag = saveChildTag("자바", parentTag);
        VideoMetadata vm = saveVideo(uploader.getId(), "자바 강의", 500, 300);
        videoTagRepository.save(new VideoTag(vm, javaTag));

        em.flush();
        em.clear();

        List<TagVideoResponse> result = tagService.getVideosByTag(javaTag.getId(), viewer.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).watchProgressPercent()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("해당 태그에 연결된 영상이 없으면 빈 리스트를 반환")
    void 태그에_영상_없으면_빈리스트_반환() {
        User viewer = saveUser("viewer@test.com", "시청자");
        Tag parentTag = saveParentTag("기술");
        Tag javaTag = saveChildTag("자바", parentTag);

        em.flush();
        em.clear();

        List<TagVideoResponse> result = tagService.getVideosByTag(javaTag.getId(), viewer.getId());

        assertThat(result).isEmpty();
    }

}