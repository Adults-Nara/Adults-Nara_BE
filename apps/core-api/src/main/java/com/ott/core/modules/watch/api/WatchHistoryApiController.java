package com.ott.core.modules.watch.api;

import com.ott.core.modules.watch.service.WatchHistoryRedisService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WatchHistoryApiController {

    private final WatchHistoryRedisService watchHistoryService;

    public WatchHistoryApiController(WatchHistoryRedisService watchHistoryService) {
        this.watchHistoryService = watchHistoryService;
    }
}
