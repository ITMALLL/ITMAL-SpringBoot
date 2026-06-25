package com.itmal.global.exception;

import com.itmal.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@RestControllerAdvice(annotations = {org.springframework.web.bind.annotation.RestController.class})
public class RestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidException(
            MethodArgumentNotValidException e) {
        String message = Objects.requireNonNull(e.getBindingResult()
                        .getFieldError())
                .getDefaultMessage();

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;  // ← SERVER_ERROR → INVALID_REQUEST

        return ResponseEntity.status(errorCode.getStatus())
                .body(new ApiResponse<>(
                        errorCode.getCode(),
                        message,  // ← errorCode.getMessage() → message
                        null
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        ErrorCode errorCode = ErrorCode.SERVER_ERROR;  // ← INVALID_REQUEST → SERVER_ERROR

        return ResponseEntity.status(errorCode.getStatus())
                .body(new ApiResponse<>(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        null
                ));
    }
    // ← 추가: ApiException 처리
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException e) {
        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity.status(errorCode.getStatus())
                .body(new ApiResponse<>(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        null
                ));
    }
}