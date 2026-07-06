package com.itmal.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;


@Slf4j
@ControllerAdvice(annotations = {Controller.class})

public class ViewExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseBody
    public ResponseEntity<String> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body("첨부파일 용량이 제한(개별 50MB, 전체 160MB)을 초과했습니다.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(e.getMessage());
    }

    @ExceptionHandler(ViewException.class)
    public ModelAndView handleViewException(ViewException e) { // @ResponseBody 없음 → HTML 뷰 렌더링
        ErrorCode errorCode = e.getErrorCode();
        ModelAndView mav = new ModelAndView("error/error"); // templates/error/error.html
        mav.setStatus(errorCode.getStatus());               // 예: 404
        mav.addObject("status", errorCode.getStatus().value());
        mav.addObject("message", errorCode.getMessage());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception e) {
        log.error("[View] 처리되지 않은 예외 발생", e);
        ModelAndView mav = new ModelAndView("error/error");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        mav.addObject("message", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        return mav;
    }

}
