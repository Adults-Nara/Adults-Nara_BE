package com.ott.common.response;

import java.util.List;

public class ErrorMessage {

    private final String code;
    private final String message;
    private final List<FieldError> errors;

    public ErrorMessage(String code, String message) {
        this(code, message, null);
    }

    public ErrorMessage(String code, String message, List<FieldError> errors) {
        this.code = code;
        this.message = message;
        this.errors = errors;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public List<FieldError> getErrors() { return errors; }

    public static class FieldError {
        private final String field;
        private final String reason;

        public FieldError(String field, String reason) {
            this.field = field;
            this.reason = reason;
        }

        public String getField() { return field; }
        public String getReason() { return reason; }
    }
}