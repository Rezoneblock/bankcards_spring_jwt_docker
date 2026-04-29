package com.gordeev.bankcards.dto.card;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CardResponse(
        Long id,
        String maskedNumber,
        String ownerName,
        LocalDate expirationDate,
        BigDecimal balance,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
