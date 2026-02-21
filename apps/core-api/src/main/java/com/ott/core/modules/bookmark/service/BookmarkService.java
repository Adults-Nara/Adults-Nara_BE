package com.ott.core.modules.bookmark.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.Bookmark;
import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.core.modules.bookmark.repository.BookmarkRepository;
import com.ott.core.modules.user.repository.UserRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final VideoMetadataRepository videoMetadataRepository;

    public void toggleBookmark(Long userId, Long videoMetadataId) {
        User user = findUser(userId);
        VideoMetadata video = findVideo(videoMetadataId);

        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserAndVideoMetadata(user, video);

        if (existingBookmark.isPresent()) {
            // 이미 찜했으면 -> 취소
            bookmarkRepository.delete(existingBookmark.get());
            videoMetadataRepository.decreaseBookmarkCount(videoMetadataId);
        } else {
            // 없으면 -> 찜하기
            Bookmark newBookmark = new Bookmark(user, video);
            bookmarkRepository.save(newBookmark);
            videoMetadataRepository.increaseBookmarkCount(videoMetadataId);
        }
    }
    // 조회
    @Transactional(readOnly = true)
    public boolean isBookmarked(Long userId, Long videoMetadataId) {
        return bookmarkRepository.findByUserIdAndVideoId(userId, videoMetadataId).isPresent();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private VideoMetadata findVideo(Long videoMetadataId) {
        return videoMetadataRepository.findById(videoMetadataId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
    }
}
