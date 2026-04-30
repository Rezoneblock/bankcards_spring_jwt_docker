package com.gordeev.bankcards.dto.card;

import com.gordeev.bankcards.enums.BlockRequestStatus;

import java.time.OffsetDateTime;

public record CardBlockResponse(
        Long id,
        Long cardId,
        String maskedNumber,
        String reason,
        BlockRequestStatus status,
        OffsetDateTime requestedAt,
        OffsetDateTime processedAt
) {
}
