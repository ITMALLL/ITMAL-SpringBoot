package com.itmal.papago.controller;

import com.itmal.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.itmal.papago.dto.PapagoRequestDto;
import com.itmal.papago.service.PapagoService;
import java.util.Map;

@RestController
@RequestMapping("/api/papago")
public class PapagoController {

    private final PapagoService papagoService;

    public PapagoController(PapagoService papagoService) {
        this.papagoService = papagoService;
    }

    @PostMapping("/translate")
    public ResponseEntity<ApiResponse<Map<String, String>>> translate(
            //valid 검증 실패 시 -> RestExceptionHandler.handleValidException()
            @Valid @RequestBody PapagoRequestDto request) {
        String result = papagoService.translate(request);
        return ResponseEntity.ok(
                new ApiResponse<>("SUCCESS", "성공", Map.of("translatedText", result))
        );
    }
}