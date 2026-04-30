package com.gordeev.bankcards.dto.card;

import jakarta.validation.constraints.NotNull;

public record CardBlockRequest(
        @NotNull(message = "ID карты обязателен") Long cardId,
        String reason
) {
}
