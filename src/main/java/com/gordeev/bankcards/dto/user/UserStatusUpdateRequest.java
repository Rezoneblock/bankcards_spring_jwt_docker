package com.gordeev.bankcards.dto.user;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UserStatusUpdateRequest(
        @NotNull(message = "ID пользователя обязателен")
        UUID userId,

        @NotNull(message = "Статус enabled обязателен")
        boolean enabled
) {
}
