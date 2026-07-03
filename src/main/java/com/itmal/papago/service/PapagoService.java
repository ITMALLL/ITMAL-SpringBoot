package com.itmal.papago.service;

import com.itmal.global.exception.ApiException;
import com.itmal.global.exception.ErrorCode;
import com.itmal.papago.dto.PapagoErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.itmal.papago.dto.PapagoRequestDto;
import com.itmal.papago.dto.PapagoResponseDto;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PapagoService {

    private final WebClient webClient;

    @Value("${papago.api.url}")
    private String papagoApiUrl;

    @Value("${papago.api.client-id}")
    private String clientId;

    @Value("${papago.api.client-secret}")
    private String clientSecret;

    public String translate(PapagoRequestDto request) {
        return webClient.post()
                .uri(papagoApiUrl)
                .header("X-NCP-APIGW-API-KEY-ID", clientId)
                .header("X-NCP-APIGW-API-KEY", clientSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                        response -> response.bodyToMono(PapagoErrorResponse.class)
                                .flatMap(errorBody -> {
                                    // Papago 에러 코드로 ErrorCode 찾기
                                    String errorCode = errorBody.getError().getErrorCode();
                                    ErrorCode code = ErrorCode.valueOf(errorCode);
                                    return Mono.error(new ApiException(code));
                                }))
                .bodyToMono(PapagoResponseDto.class)
                .map(PapagoResponseDto::getTranslatedText)
                .block(Duration.ofSeconds(10));
    }
}