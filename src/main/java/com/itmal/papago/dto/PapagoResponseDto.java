package com.itmal.papago.dto;

import lombok.Getter;
import lombok.Setter;
//응답 형식
//{
//"message": {
//    "result": {
//        "translatedText": "Hello"
//        }
//    }
//}
@Setter
@Getter
public class PapagoResponseDto {
    private MessageDto message;

    @Setter
    @Getter
    public static class MessageDto {
        private ResultDto result;

    }

    @Setter
    @Getter
    public static class ResultDto {
        private String translatedText;

    }

    // 편의 메서드
    public String getTranslatedText() {
        if (message != null && message.result != null) {
            return message.result.getTranslatedText();
        }
        return null;
    }
}