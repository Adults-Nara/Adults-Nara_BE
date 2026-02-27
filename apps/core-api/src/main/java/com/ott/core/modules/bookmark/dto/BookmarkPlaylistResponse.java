package com.ott.core.modules.bookmark.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class BookmarkPlaylistResponse {
    private long totalCount;
    private List<String> thumbnails;
}
