package com.gordeev.bankcards.dto.card;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CardTransferRequest(
        @NotNull(message = "ID карты отправителя обязателен")
        Long fromCardId,

        @NotNull(message = "ID карты получателя обязателен")
        Long toCardId,

        @NotNull(message = "Сумма перевода обязательна")
        @Positive(message = "Сумма перевода должна быть больше нуля")
        BigDecimal amount
) {
}
