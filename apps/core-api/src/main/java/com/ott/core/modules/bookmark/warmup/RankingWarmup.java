package com.ott.core.modules.bookmark.warmup;

import com.ott.common.persistence.entity.VideoMetadata;
import com.ott.core.modules.video.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingWarmup implements ApplicationRunner {

    private final VideoMetadataRepository videoMetadataRepository;
    private final StringRedisTemplate stringRedisTemplate;

    // Redis Key 설정
    private static final String TYPE = "bookmark";
    private static final String KEY_VIDEO_COUNT = "video:count:" + TYPE;
    private static final String KEY_RANKING = "video:ranking";

    private static final int CHUNK_SIZE = 10000;

    /**
     * 서버가 켜진 후, 1번만 자동으로 실행
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("🚀 [Cache Warm-up] 서버 구동 완료. 실시간 인기 랭킹 캐시 사전 적재를 시작합니다...");
        long startTime = System.currentTimeMillis();

        // 기존 Redis 랭킹 데이터를 비운다.
        stringRedisTemplate.delete(List.of(KEY_VIDEO_COUNT, KEY_RANKING));

        int page = 0;
        long totalProcessed = 0;

        while (true) {

            PageRequest pageRequest = PageRequest.of(page, CHUNK_SIZE);
            Page<VideoMetadata> videoPage = videoMetadataRepository.findAll(pageRequest);

            List<VideoMetadata> videos = videoPage.getContent();

            if (videos.isEmpty()) {
                break;
            }

            // redis 파이프라인을 청크단위로 씀
            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (VideoMetadata video : videos) {
                    String videoIdStr = String.valueOf(video.getVideoId());
                    int count = video.getBookmarkCount();

                    // 상세 페이지에 보여줄 개별 카운트 (Hash) 적재
                    connection.hashCommands().hSet(
                            KEY_VIDEO_COUNT.getBytes(),
                            videoIdStr.getBytes(),
                            String.valueOf(count).getBytes()
                    );

                    // 실시간 인기 차트 (ZSet) 적재
                    connection.zSetCommands().zAdd(
                            KEY_RANKING.getBytes(),
                            count, // Score (북마크 개수)
                            videoIdStr.getBytes()
                    );
                }
                return null;
            });

            totalProcessed += videos.size();
            log.info("   -> [Cache Warm-up] {} / {} 개 적재 완료...", totalProcessed, videoPage.getTotalElements());

            // 마지막 페이지면 종료
            if (!videoPage.hasNext()) {
                break;
            }
            page++;
        }

        long endTime = System.currentTimeMillis();
        log.info("🚀 [Cache Warm-up] 완벽 적재 완료! 총 {}개의 비디오 캐시 (소요 시간: {}ms)", totalProcessed, (endTime - startTime));
    }
}