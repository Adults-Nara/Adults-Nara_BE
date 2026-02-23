package com.ott.core.modules.bookmark.dto;

public record BookmarkStatusResponseDto(
        boolean isBookmarked
) {
    public static BookmarkStatusResponseDto from(boolean status) {
        return new BookmarkStatusResponseDto(status);
    }
}
