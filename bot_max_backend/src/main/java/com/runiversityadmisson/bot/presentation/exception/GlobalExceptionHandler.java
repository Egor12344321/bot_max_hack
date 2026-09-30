package com.runiversityadmisson.bot.presentation.exception;


import com.runiversityadmisson.bot.application.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Глобальный обработчик исключений для REST API.
 * Возвращает единый формат ошибок: {code, message}.
 * message показывается пользователю в мини-аппе, поэтому всегда по-русски;
 * технические подробности пишутся только в лог.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(OnboardingRequiredException.class)
    public ResponseEntity<ErrorResponse> handleOnboardingRequired(OnboardingRequiredException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("onboarding_required", exception.getMessage()));
    }

    /** initData невалиден → 401 Unauthorized. */
    @ExceptionHandler(InvalidInitDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInitData(
            InvalidInitDataException exception,
            HttpServletRequest request) {
        log.warn("Invalid initData на {}: {}", request.getRequestURI(), exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("invalid_init_data",
                        "Не удалось подтвердить вход через MAX. Откройте приложение заново из бота."));
    }

    /** JWT невалиден → 401 Unauthorized. */
    @ExceptionHandler(InvalidJwtException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJwt(
            InvalidJwtException exception,
            HttpServletRequest request) {
        log.warn("Invalid JWT на {}: {}", request.getRequestURI(), exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("invalid_jwt", "Сессия истекла. Откройте приложение заново из бота."));
    }

    /** Ошибки валидации DTO (@Valid) → 400 Bad Request. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> "Некорректное поле «" + error.getField() + "»: " + error.getDefaultMessage())
                .findFirst()
                .orElse("Некорректные данные запроса");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("validation_failed", message));
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException exception) {
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(new ErrorResponse("bad_request", exception.getMessage()));
	}

    /** Данные изменились с момента загрузки (версия плана) → 409 Conflict. */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("version_conflict", exception.getMessage()));
    }

    /** Ресурс не найден → 404 Not Found. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("not_found", exception.getMessage()));
    }

    /** Нет обязательного параметра, параметр не того типа или тело запроса не JSON → 400. */
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception exception, HttpServletRequest request) {
        log.warn("Некорректный запрос на {}: {}", request.getRequestURI(), exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("validation_failed", "Некорректные параметры запроса"));
    }

    /** Неизвестный адрес → 404, а не 500. Так отвечают и разделы, которых на сервере ещё нет. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleUnknownPath(NoResourceFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("not_found", "Этот раздел пока недоступен на сервере"));
    }

    /** Всё остальное → 500 Internal Server Error. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception exception,
            HttpServletRequest request) {
        log.error("Unexpected error на {}", request.getRequestURI(), exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("internal_error", "Что-то пошло не так. Попробуйте ещё раз."));
    }
}
