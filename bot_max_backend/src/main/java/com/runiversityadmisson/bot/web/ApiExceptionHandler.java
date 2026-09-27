package com.runiversityadmisson.bot.web;

import com.runiversityadmisson.bot.web.auth.InvalidInitDataException;
import com.runiversityadmisson.bot.web.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(InvalidInitDataException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidInitData(InvalidInitDataException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new ApiErrorResponse("INVALID_INIT_DATA", exception.getMessage()));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException exception) {
		return ResponseEntity.status(exception.getStatusCode())
				.body(new ApiErrorResponse("REQUEST_REJECTED", exception.getReason()));
	}
}
