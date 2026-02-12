package com.ott.common.response;

import com.ott.common.error.ErrorCode;

public class ApiResponse<T> {
    private final ResultType result;
    private final T data;
    private final ErrorMessage error;

    private ApiResponse(ResultType result, T data, ErrorMessage error) {
        this.result = result;
        this.data = data;
        this.error = error;
    }

    public static ApiResponse<?> success() {
        return new ApiResponse<>(ResultType.SUCCESS, null, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultType.SUCCESS, data, null);
    }

    public static ApiResponse<?> error(String code, String message) {
        return new ApiResponse<>(ResultType.ERROR, null, new ErrorMessage(code, message));
    }

    public static ApiResponse<?> error(ErrorCode errorCode) {
        return new ApiResponse<>(ResultType.ERROR, null,
                new ErrorMessage(errorCode.getCode(), errorCode.getMessage()));
    }

    public ResultType getResult() { return result; }
    public T getData() { return data; }           // (권장: Object -> T)
    public ErrorMessage getError() { return error; }
}
