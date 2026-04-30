package com.gordeev.bankcards.dto.card;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CardCreateRequest(
        @NotBlank @Pattern(regexp = "\\d{16}", message = "Номер карты должен содержать 16 цифр") String cardNumber,
        @NotBlank @Pattern(regexp = "^[A-Z]+(?: [A-Z]+)+$", message = "Имя владельца должно быть формата: IVANOV IVAN") @Size(max = 100) String ownerName,
        @NotNull @PositiveOrZero BigDecimal initialBalance,
        @NotNull UUID userId
        ) {
}
