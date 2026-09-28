package com.runiversityadmisson.bot.presentation.exception;


import com.runiversityadmisson.bot.application.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Глобальный обработчик исключений для REST API.
 * Возвращает единый формат ошибок: {code, message}.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** initData невалиден → 401 Unauthorized. */
    @ExceptionHandler(InvalidInitDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInitData(
            InvalidInitDataException exception,
            HttpServletRequest request) {
        log.warn("Invalid initData на {}: {}", request.getRequestURI(), exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("invalid_init_data", exception.getMessage()));
    }

    /** JWT невалиден → 401 Unauthorized. */
    @ExceptionHandler(InvalidJwtException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJwt(
            InvalidJwtException exception,
            HttpServletRequest request) {
        log.warn("Invalid JWT на {}: {}", request.getRequestURI(), exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("invalid_jwt", exception.getMessage()));
    }

    /** Ошибки валидации DTO (@Valid) → 400 Bad Request. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("validation_failed", message));
    }

    /** Ресурс не найден → 404 Not Found. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("not_found", exception.getMessage()));
    }

    /** Всё остальное → 500 Internal Server Error. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception exception,
            HttpServletRequest request) {
        log.error("Unexpected error на {}", request.getRequestURI(), exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("internal_error", "Something went wrong"));
    }
}
