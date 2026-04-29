package com.gordeev.bankcards.dto.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final Boolean success;
    private final T data;
    private final ApiError error;
    private final LocalDateTime timestamp = LocalDateTime.now();

    private ApiResponse(Boolean success, T data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <D> ApiResponse<D> success(D data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <D> ApiResponse<D> error(ApiError error) {
        return new ApiResponse<>(false, null, error);
    }
}
