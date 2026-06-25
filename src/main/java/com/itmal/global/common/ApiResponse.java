package com.itmal.global.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ApiResponse<T> {
    private String code;
    private String message;
    private T data;
}
