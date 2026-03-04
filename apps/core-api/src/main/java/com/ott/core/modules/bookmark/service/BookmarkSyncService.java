package com.ott.core.modules.bookmark.service;

import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.core.modules.bookmark.repository.BookmarkRepository;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkSyncService {

    private final StringRedisTemplate stringRedisTemplate;
    private final VideoMetadataRepository videoMetadataRepository;

    private static final String KEY_RANKING = "video:ranking"; // ZSet
    private static final String KEY_DIRTY = "video:dirty:bookmark"; // Set
    private static final String KEY_PROCESSING = "video:processing:bookmark"; // Set (안전 큐)
    private final BookmarkRepository bookmarkRepository;

    /**
     * Redis에 저장된 북마크 카운트를 DB(VideoMetadata)에 동기화하는 비즈니스 로직
     */
    public void syncBookmarkCounts() {
        // 1. [장애 복구] 이전 작업 중 서버가 뻗었다면 다시 합침
            stringRedisTemplate.opsForSet().unionAndStore(KEY_DIRTY, KEY_PROCESSING, KEY_DIRTY);
            stringRedisTemplate.delete(KEY_PROCESSING);
        }

        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(KEY_DIRTY))) {
            return; // 동기화할 데이터가 없으면 조용히 종료
        }

        // 2. [동시성 방어] 대기열을 작업장(processing)으로 원자적 이동
        try {
            stringRedisTemplate.rename(KEY_DIRTY, KEY_PROCESSING);
        } catch (Exception e) {
            return;
        }

        Set<String> videoIdsToProcess = stringRedisTemplate.opsForSet().members(KEY_PROCESSING);

        if (videoIdsToProcess == null || videoIdsToProcess.isEmpty()) {
            stringRedisTemplate.delete(KEY_PROCESSING);
            return;
        }

        log.info("[SyncService] 북마크 카운트 DB 동기화 시작 (대상: {}건)", videoIdsToProcess.size());
        int successCount = 0;

        for (String videoIdStr : videoIdsToProcess) {
            try {
                Long videoId = Long.valueOf(videoIdStr);

                // 3. Redis ZSet에서 현재 이 비디오의 북마크 카운트를 가져옵니다.
                Double score = stringRedisTemplate.opsForZSet().score(KEY_RANKING, videoIdStr);

                if (score != null) {
                    // 4. Repository에 이미 @Transactional이 있으므로 안전하게 업데이트 됨
                    videoMetadataRepository.updateBookmarkCount(videoId, score.intValue());
                    successCount++;
                }

            } catch (Exception e) {
                log.error("[SyncService] 비디오 {} 북마크 동기화 중 에러 발생: {}", videoIdStr, e.getMessage());
                // 에러 발생 시, 유실되지 않게 다음 턴에 다시 시도하도록 dirty에 다시 넣음
                stringRedisTemplate.opsForSet().add(KEY_DIRTY, videoIdStr);
            }
        }

        // 5. 완료된 큐 삭제
        stringRedisTemplate.delete(KEY_PROCESSING);

        if (successCount > 0) {
            log.info("[SyncService] 북마크 카운트 {}건 DB 완벽 동기화 완료!", successCount);
        }
    }

    /**
     * [DB -> Redis] 수동 DB 조작이나 Redis 초기화(장애) 시 랭킹 데이터를 복구하는 메서드
     * 관리자(Admin) API나 서버 기동 시 호출하도록 설계
     */
    @Transactional(readOnly = true)
    public void warmUpRankingFromDB() {
        log.info("[Sync] DB 데이터를 기반으로 Redis 랭킹(ZSet) 초기화를 시작합니다...");
        stringRedisTemplate.delete("video:ranking"); // 기존 캐시 날림

        // DB 딱 1번 찌름! (N+1 완벽 해결)
        List<Object[]> results = bookmarkRepository.countTotalBookmarksGroupedByVideo();

        int count = 0;
        for (Object[] row : results) {
            Long videoId = (Long) row[0];
            Long bookmarkCount = (Long) row[1];

            if (bookmarkCount > 0) {
                stringRedisTemplate.opsForZSet().add("video:ranking", String.valueOf(videoId), bookmarkCount.doubleValue());
                count++;
            }
        }
        log.info("[Sync] 총 {}개 비디오의 랭킹 데이터가 복구되었습니다!", count);
    }
}
