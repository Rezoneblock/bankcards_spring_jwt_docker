package com.gordeev.bankcards.dto.card;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CardCreateRequest(
        @NotBlank @Pattern(regexp = "\\d{16}", message = "Номер карты должен содержать 16 цифр") String cardNumber,
        @NotBlank @Size(max = 150) String ownerName,
        @NotNull @Pattern(regexp = "\\d{2}/\\d{2}", message = "Формат срока действия: mm/yy") LocalDate expirationDate,
        @NotNull @Positive BigDecimal initialBalance,
        @NotNull UUID userId
        ) {
}
