package com.ott.core.modules.watch.api;

import com.ott.core.modules.watch.service.WatchHistoryRedisService;
import com.ott.core.modules.watch.service.WatchHistoryService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WatchHistoryApiController {

    private final WatchHistoryService watchHistoryService;

    public WatchHistoryApiController(WatchHistoryService watchHistoryService) {
        this.watchHistoryService = watchHistoryService;
    }
}
