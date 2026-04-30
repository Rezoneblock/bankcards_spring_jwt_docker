package com.gordeev.bankcards.exception;

import com.gordeev.bankcards.dto.api.ApiError;
import com.gordeev.bankcards.dto.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Обработка IllegalArgumentException
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        ApiError error = new ApiError(
                ex.getMessage(),
                "ILLEGAL_ARGUMENT",
                null
        );

        return ResponseEntity.badRequest().body(ApiResponse.error(error));
    }

    // Обработка IllegalStateException
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        ApiError error = new ApiError(
                ex.getMessage(),
                "ILLEGAL_STATE",
                null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
    }

    // Обработка ошибки тела запроса
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String message = "Тело запроса отсутствует или имеет неверный формат";

        if (ex.getMessage() != null && ex.getMessage().contains("Required request body is missing")) {
            message = "Тело запроса обязательно для этого endpoint";
        }

        ApiError error = new ApiError(
                message,
                "REQUEST_BODY_MISSING",
                null
        );

        return ResponseEntity.badRequest().body(ApiResponse.error(error));
    }

    // Валидация запросов
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
            }
        });

        ApiError error = new ApiError(
                "Ошибка валидации тела запроса",
                "VALIDATION_ERROR",
                errors
        );

        return ResponseEntity.badRequest().body(ApiResponse.error(error));
    }

    // Моё кастомное исключение (businessException)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> businessHandler(BusinessException ex) {
        ApiError error = new ApiError(
                ex.getMessage(),
                ex.getErrorCode(),
                null
        );

        return ResponseEntity.status(ex.getHttpStatus()).body(ApiResponse.error(error));
    }

    // Обработка отсутствия обязательного параметра запроса
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex) {

        String message = String.format("Отсутствует обязательный параметр: %s (тип: %s)",
                ex.getParameterName(),
                ex.getParameterType());

        Map<String, String> details = Map.of(
                "parameter", ex.getParameterName(),
                "type", ex.getParameterType(),
                "required", "true"
        );

        ApiError error = new ApiError(
                message,
                "MISSING_PARAMETER",
                details
        );

        return ResponseEntity.badRequest().body(ApiResponse.error(error));
    }
}
