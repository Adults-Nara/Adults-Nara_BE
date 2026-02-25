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
    USER_DUPLICATE_EMAIL("USER-409-001", 409, "이미 사용 중인 이메일입니다."),
    USER_CANNOT_BAN_ADMIN("USER-403-001", 403, "관리자는 정지할 수 없습니다."),
    USER_CANNOT_DELETE_ADMIN("USER-403-002", 403, "관리자는 삭제할 수 없습니다."),

    VIDEO_INVALID_TITLE("VIDEO-400-TITLE", 400, "잘못된 제목입니다. 제목은 2글자 이상 100글자 이하여야 합니다."),
    VIDEO_INVALID_DESCRIPTION("VIDEO-400-DESCRIPTION", 400, "잘못된 설명입니다. 설명은 0글자 이상 4000글자 이하여야 합니다."),
    VIDEO_SESSION_EXPIRED("VIDEO-400-003", 400, "만료된 비디오 세션입니다."),
    VIDEO_MISMATCH("VIDEO-400-004", 400, "비디오 영상 관련 값이 적절하지 않습니다."),
    VIDEO_INVALID_SIZE("VIDEO-400-005", 400, "비디오 사이즈가 올바르지 않습니다."),
    VIDEO_NOT_PUBLIC("VIDEO-403-001", 403, "비디오가 public이 아닙니다."),
    VIDEO_NOT_READY("VIDEO-403-002", 403, "비디오가 준비되지 않았습니다."),
    VIDEO_NOT_FOUND("VIDEO-404-001", 404, "요청한 비디오를 찾을 수 없습니다."),


    DAILY_LIMIT_OVER("POINT-400-001", 400, "오늘 참여 가능한 광고 시청 횟수를 모두 채우셨습니다."),
    DUPLICATE_AD_REWARD("POINT-409-001", 409, "이미 포인트 적립이 완료된 영상입니다."),
    DUPLICATE_PURCHASE_REWARD("POINT-409-002", 409, "이미 포인트 적립이 완료된 주문입니다."),
    POINT_INSUFFICIENT_BALANCE("POINT-400-002", 400, "기프티콘 구매를 위한 포인트가 부족합니다."),
    GIFTICON_OUT_OF_STOCK("POINT-500-001", 500, "현재 기프티콘 구매가 불가합니다."),

    IO_EXCEPTION("IOE-500-001", 400, "IOException이 발생했습니다.");
    // ===== Bookmark (찜하기 관련 세분화) =====
    BOOKMARK_CONFLICT("BOOKMARK-409-001", 409, "이미 처리 중이거나 완료된 찜하기 요청입니다. 잠시 후 다시 시도해주세요."),
    BOOKMARK_NOT_FOUND("BOOKMARK-404-001", 404, "해당 찜하기 내역을 찾을 수 없습니다."),

    // ===== Interaction (좋아요/싫어요 관련 세분화) =====
    INTERACTION_CONFLICT("INTERACTION-409-001", 409, "이미 처리 중이거나 완료된 반응 요청입니다. 잠시 후 다시 시도해주세요."),

    // ===== System / Sync (백그라운드 스케줄러 & Redis 세분화) =====
    REDIS_DATA_PARSING_ERROR("SYSTEM-500-001", 500, "Redis 캐시 데이터 파싱 중 오류가 발생했습니다."),
    DB_SYNC_ERROR("SYSTEM-500-002", 500, "상호작용 계산 데이터베이스 동기화 중 오류가 발생했습니다."),

    IO_EXCEPTION("IOE-500-001", 500, "IOException이 발생했습니다.");

    private final String code;
    private final int httpStatus;
    private final String message;

    ErrorCode(String code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}