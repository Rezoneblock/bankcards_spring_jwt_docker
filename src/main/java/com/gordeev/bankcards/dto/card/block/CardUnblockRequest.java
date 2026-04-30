package com.gordeev.bankcards.dto.card.block;

import jakarta.validation.constraints.NotNull;

public record CardUnblockRequest(
        @NotNull(message = "ID карты обязателен")
        Long cardId
) {
}
