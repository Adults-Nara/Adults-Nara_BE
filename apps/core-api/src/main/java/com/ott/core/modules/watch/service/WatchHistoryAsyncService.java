package com.ott.core.modules.watch.service;

import com.ott.common.util.IdGenerator;
import com.ott.core.modules.watch.repository.WatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatchHistoryAsyncService {

    private final WatchHistoryRepository watchHistoryRepository;

    @Async("watchHistoryTaskExecutor")
    @Transactional
    public void saveWatchHistoryToDb(Long userId, Long videoMetadataId, Integer lastPosition) {
        watchHistoryRepository.upsertWatchHistory(IdGenerator.generate(), userId, videoMetadataId, lastPosition, OffsetDateTime.now(ZoneOffset.UTC));
        log.info("DB Upserted - userId: {}, videoMetadataId: {}, position: {}", userId, videoMetadataId, lastPosition);
    }
}
