package com.itmal.global.exception;

import lombok.Getter;

@Getter
public class ViewException extends RuntimeException {
    private final ErrorCode errorCode;

    public ViewException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
