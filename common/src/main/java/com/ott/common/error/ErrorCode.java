package com.ott.common.error;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // ===== Common =====
    INVALID_REQUEST("COMMON-400", 400, "잘못된 요청입니다."),
    UNAUTHORIZED("COMMON-401", 401, "인증이 필요합니다."),
    FORBIDDEN("COMMON-403", 403, "권한이 없습니다."),
    NOT_FOUND("COMMON-404", 404, "요청한 리소스를 찾을 수 없습니다."),
    CONFLICT("COMMON-409", 409, "리소스 충돌이 발생했습니다."),
    INTERNAL_ERROR("COMMON-500", 500, "서버 내부 오류입니다."),

    /**
     * 공통 에러 타입 정의
     *
     * | 타입 | 설명 | HTTP 상태 코드 |
     * |------|------|----------------|
     * | Bad Request | 잘못된 요청 | 400 |
     * | Unauthorized | 인증되지 않은 요청 | 401 |
     * | Forbidden | 권한이 없는 요청 | 403 |
     * | Not Found | 요청한 리소스를 찾을 수 없음 | 404 |
     * | Conflict | 리소스 충돌 발생 | 409 |
     * | Internal Server Error | 서버 내부 오류 | 500 |
     */

    // ===== User =====
    USER_NOT_FOUND("USER-404-001", 404, "사용자를 찾을 수 없습니다."),
    USER_INVALID_CREDENTIAL("USER-401-001", 401, "이메일 또는 비밀번호가 올바르지 않습니다."),
    USER_CANNOT_BAN_ADMIN("USER-403-001", 403, "관리자는 정지할 수 없습니다."),
    USER_CANNOT_DELETE_ADMIN("USER-403-002", 403, "관리자는 삭제할 수 없습니다."),

    VIDEO_INVALID_TITLE("VIDEO-400-TITLE", 400, "잘못된 제목입니다. 제목은 2글자 이상 100글자 이하여야 합니다."),
    VIDEO_INVALID_DESCRIPTION("VIDEO-400-DESCRIPTION", 400, "잘못된 설명입니다. 설명은 0글자 이상 4000글자 이하여야 합니다."),
    VIDEO_NOT_FOUND("VIDEO-404-001", 404, "요청한 비디오를 찾을 수 없습니다.");

    private final String code;
    private final int httpStatus;
    private final String message;

    ErrorCode(String code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}