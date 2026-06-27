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
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CM001", "댓글을 찾을 수 없습니다."),
    COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "CM002", "본인 댓글만 수정/삭제할 수 있습니다."),

    // Auth
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "A001", "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "A002", "이미 사용 중인 닉네임입니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "A003", "이메일 발송에 실패했습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "A004", "이메일 발송은 60초에 한 번만 가능합니다."),

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