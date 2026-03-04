package com.ott.core.modules.comment.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.Comment;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.common.persistence.enums.UserRole;
import com.ott.common.util.IdGenerator;
import com.ott.core.modules.comment.dto.CommentCreateRequest;
import com.ott.core.modules.comment.dto.CommentEditRequest;
import com.ott.core.modules.comment.dto.CommentPageResponse;
import com.ott.core.modules.comment.dto.MyCommentResponse;
import com.ott.core.modules.comment.repository.CommentRepository;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import com.ott.core.modules.video.service.SignedCookieProcessor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class CommentServiceTest {

    @MockitoBean private SignedCookieProcessor signedCookieProcessor;
    @MockitoBean private software.amazon.awssdk.services.s3.S3Client s3Client;
    @MockitoBean private software.amazon.awssdk.services.s3.presigner.S3Presigner s3Presigner;

    @Autowired private EntityManager em;
    @Autowired private CommentService commentService;
    @Autowired private CommentRepository commentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private VideoMetadataRepository videoMetadataRepository;

    private User saveUser(String email, String nickname) {
        return userRepository.save(new User(email, nickname, "pw", UserRole.VIEWER));
    }

    private VideoMetadata saveVideoMetadata() {
        return videoMetadataRepository.save(VideoMetadata.builder()
                .videoId(IdGenerator.generate())
                .userId(IdGenerator.generate())
                .title("테스트 영상")
                .commentCount(0)
                .deleted(false)
                .build());
    }

    private Comment saveComment(User user, VideoMetadata vm, String text) {
        return commentRepository.save(new Comment(vm, user, text));
    }

    private CommentCreateRequest createRequest(String text) {
        CommentCreateRequest req = new CommentCreateRequest();
        ReflectionTestUtils.setField(req, "text", text);
        return req;
    }

    private CommentEditRequest editRequest(String text) {
        CommentEditRequest req = new CommentEditRequest();
        ReflectionTestUtils.setField(req, "text", text);
        return req;
    }

    @Test
    @DisplayName("로그인 사용자 댓글 조회 - 내 댓글은 myComment로 분리되고 comments 목록에서 제외")
    void 로그인_댓글목록_내댓글_분리() {
        User me = saveUser("me@test.com", "나");
        User other = saveUser("other@test.com", "타인");
        VideoMetadata vm = saveVideoMetadata();
        saveComment(me, vm, "내 댓글");
        saveComment(other, vm, "타인 댓글");
        em.flush();
        em.clear();

        CommentPageResponse response = commentService.getComments(me.getId(), vm.getVideoId(), 0, 20);

        assertThat(response.getMyComment()).isNotNull();
        assertThat(response.getMyComment().getText()).isEqualTo("내 댓글");
        assertThat(response.getComments()).hasSize(1);
        assertThat(response.getComments().getFirst().getText()).isEqualTo("타인 댓글");
    }

    @Test
    @DisplayName("비로그인 댓글 조회 - myComment는 null이고 전체 목록 반환")
    void 비로그인_댓글조회() {
        User user = saveUser("me@test.com", "유저");
        VideoMetadata vm = saveVideoMetadata();
        saveComment(user, vm, "댓글1");
        saveComment(user, vm, "댓글2");
        em.flush();
        em.clear();

        CommentPageResponse response = commentService.getComments(null, vm.getVideoId(), 0, 20);

        assertThat(response.getMyComment()).isNull();
        assertThat(response.getComments()).hasSize(2);
    }

    @Test
    @DisplayName("삭제된 댓글은 목록에서 제외")
    void 삭제된_댓글_조회_제외() {
        User user = saveUser("u@test.com", "유저");
        VideoMetadata vm = saveVideoMetadata();
        saveComment(user, vm, "정상 댓글");
        Comment deleted = saveComment(user, vm, "삭제된 댓글");
        deleted.softDelete();
        em.flush();
        em.clear();

        CommentPageResponse response = commentService.getComments(null, vm.getVideoId(), 0, 20);

        assertThat(response.getComments()).hasSize(1);
        assertThat(response.getComments().getFirst().getText()).isEqualTo("정상 댓글");
    }

    @Test
    @DisplayName("내 댓글이 있으면 반환")
    void 내댓글_반환() {
        User user = saveUser("me@test.com", "나");
        VideoMetadata vm = saveVideoMetadata();
        saveComment(user, vm, "내 댓글");
        em.flush();
        em.clear();

        MyCommentResponse response = commentService.getMyComment(user.getId(), vm.getVideoId());

        assertThat(response).isNotNull();
        assertThat(response.getText()).isEqualTo("내 댓글");
    }

    @Test
    @DisplayName("내 댓글이 없으면 null 반환")
    void 내댓글_없으면_null반환() {
        User me = saveUser("me@test.com", "나");
        User other = saveUser("other@test.com", "타인");
        VideoMetadata vm = saveVideoMetadata();
        saveComment(other, vm, "타인 댓글");
        em.flush();
        em.clear();

        MyCommentResponse response = commentService.getMyComment(me.getId(), vm.getVideoId());

        assertThat(response).isNull();
    }

    @Test
    @DisplayName("댓글 작성 성공 - DB 저장 및 commentCount 1 증가")
    void 댓글작성_성공() {
        User user = saveUser("u@test.com", "유저");
        VideoMetadata vm = saveVideoMetadata();
        em.flush();
        em.clear();

        commentService.createComment(user.getId(), vm.getVideoId(), createRequest("새 댓글"));
        em.flush();
        em.clear();

        VideoMetadata updated = videoMetadataRepository.findByVideoId(vm.getVideoId()).orElseThrow();
        assertThat(updated.getCommentCount()).isEqualTo(1);
        assertThat(commentRepository.existsByUserIdAndVideoId(user.getId(), vm.getVideoId())).isTrue();
    }

    @Test
    @DisplayName("댓글 중복 불가")
    void 댓글중복_예외() {
        User user = saveUser("u@test.com", "유저");
        VideoMetadata vm = saveVideoMetadata();
        saveComment(user, vm, "댓글 작성");
        em.flush();
        em.clear();

        assertThatThrownBy(() -> commentService.createComment(user.getId(), vm.getVideoId(), createRequest("댓글 작성")))
                .isInstanceOf(BusinessException.class).hasMessageContaining(ErrorCode.COMMENT_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("댓글 수정 테스트")
    void 댓글수정_성공() {
        User user = saveUser("u@test.com", "유저");
        VideoMetadata vm = saveVideoMetadata();
        Comment comment = saveComment(user, vm, "원본 댓글");
        em.flush();
        em.clear();

        MyCommentResponse response = commentService.editComment(user.getId(), comment.getId(), editRequest("수정 댓글"));

        assertThat(response.getText()).isEqualTo("수정 댓글");
    }

    @Test
    @DisplayName("타인 댓글 수정 불가")
    void 타인댓글_수정불가() {
        User user = saveUser("u@test.com", "유저");
        User me = saveUser("me@test.com", "나");
        VideoMetadata vm = saveVideoMetadata();
        Comment comment = saveComment(user, vm, "댓글");
        em.flush();
        em.clear();

        assertThatThrownBy(() -> commentService.editComment(me.getId(), comment.getId(), editRequest("댓글 수정 시도")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.COMMENT_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("댓글 삭제 테스트")
    void 댓글삭제_성공() {
        User user = saveUser("u@test.com", "유저");
        VideoMetadata vm = saveVideoMetadata();
        Comment comment = saveComment(user, vm, "댓글");
        em.flush();
        em.clear();

        commentService.deleteComment(user.getId(), comment.getId());
        em.flush();
        em.clear();

        Comment deleted = commentRepository.findById(comment.getId()).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();

        VideoMetadata updated = videoMetadataRepository.findByVideoId(vm.getVideoId()).orElseThrow();
        assertThat(updated.getCommentCount()).isEqualTo(0);
    }
}