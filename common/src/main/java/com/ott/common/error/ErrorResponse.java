package com.ott.common.error;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
public class ErrorResponse {

    private final String code;
    private final String message;
    private final OffsetDateTime timestamp;
    private final String path;

    private final List<FieldError> errors;

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String reason;
    }
}