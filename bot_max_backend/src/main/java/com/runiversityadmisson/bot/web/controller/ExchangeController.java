package com.runiversityadmisson.bot.presentation.controller;


import com.runiversityadmisson.bot.web.dto.ExchangeRequest;
import com.runiversityadmisson.bot.web.dto.ExchangeResponse;
import com.runiversityadmisson.bot.web.service.ExchangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Точка входа мини-аппа: обмен initData на JWT.
 *
 * <p>Ошибки обрабатываются {@code GlobalExceptionHandler}:
 * <ul>
 *     <li>{@code InvalidInitDataException} → 401</li>
 *     <li>{@code MethodArgumentNotValidException} → 400</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/v1/auth/exchange")
public class ExchangeController {

    private final ExchangeService exchangeUseCase;

    @PostMapping
    public ResponseEntity<ExchangeResponse> exchange(
            @Valid @RequestBody ExchangeRequest request) {
        log.debug("Exchange request received");
        ExchangeResponse response = exchangeUseCase.exchange(request.launchParams());
        return ResponseEntity.ok(response);
    }
}
