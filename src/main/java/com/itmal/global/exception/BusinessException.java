package com.itmal.global.exception;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException() {
        super("질문을 찾을 수 없습니다.");
        this.errorCode = ErrorCode.INVALID_REQUEST;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
