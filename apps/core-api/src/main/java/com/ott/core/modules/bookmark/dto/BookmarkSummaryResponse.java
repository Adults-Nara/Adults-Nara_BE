package com.ott.core.modules.bookmark.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class BookmarkSummaryResponse {
    private BookmarkPlaylistResponse shortForm;
    private BookmarkPlaylistResponse longForm;
}
