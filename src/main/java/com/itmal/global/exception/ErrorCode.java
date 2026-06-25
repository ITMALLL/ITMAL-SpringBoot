package com.itmal.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 기존
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Q001", "질문을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "C001", "잘못된 요청입니다."),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "서버 오류입니다."),

    // Papago 추가
    N2MT01(HttpStatus.BAD_REQUEST, "N2MT01", "source 파라미터 누락"),
    N2MT02(HttpStatus.BAD_REQUEST, "N2MT02", "source를 지원하지 않음"),
    N2MT03(HttpStatus.BAD_REQUEST, "N2MT03", "target 파라미터 누락"),
    N2MT04(HttpStatus.BAD_REQUEST, "N2MT04", "target을 지원하지 않음"),
    N2MT05(HttpStatus.BAD_REQUEST, "N2MT05", "source와 target이 동일"),
    N2MT99(HttpStatus.INTERNAL_SERVER_ERROR, "N2MT99", "서버 내부 오류");

    private final HttpStatus status;
    private final String code;
    private final String message;
}