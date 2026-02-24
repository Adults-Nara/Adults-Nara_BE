package com.ott.core.modules.search.dto;

import java.util.List;

public record SliceResponse<T>(
        List<T> content,  // 실제 데이터 목록
        int currentPage,  // 현재 페이지 번호
        int size,         // 요청한 사이즈
        boolean hasNext   // 다음 페이지 존재 여부 (가장 중요!)
) {
    public static <T> SliceResponse<T> of(List<T> content, int currentPage, int size, boolean hasNext) {
        return new SliceResponse<>(content, currentPage, size, hasNext);
    }
}
