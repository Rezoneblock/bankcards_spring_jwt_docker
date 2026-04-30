package com.gordeev.bankcards.dto.card;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CardResponse(
        Long id,
        String maskedNumber,
        String ownerName,
        LocalDate expirationDate,
        BigDecimal balance,
        String status,
        UUID userId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
