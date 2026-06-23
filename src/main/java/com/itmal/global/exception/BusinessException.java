package com.itmal.global.exception;

public class BusinessException extends RuntimeException {

    public BusinessException() {
        super("질문을 찾을 수 없습니다.");
    }
}
