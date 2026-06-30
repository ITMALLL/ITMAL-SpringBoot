package com.itmal.papago.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PapagoErrorResponse {
    private PapagoError error;

    @Getter
    @Setter
    public static class PapagoError {
        private String errorCode;
        private String message;
    }
}