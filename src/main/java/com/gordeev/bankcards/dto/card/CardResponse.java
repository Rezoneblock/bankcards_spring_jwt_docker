package com.gordeev.bankcards.dto.card;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CardResponse(
        Long id,
        String maskedNumber,
        String ownerName,
        LocalDate expirationDate,
        @JsonInclude(JsonInclude.Include.NON_NULL) String blockReason,
        BigDecimal balance,
        String status,
        UUID userId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
