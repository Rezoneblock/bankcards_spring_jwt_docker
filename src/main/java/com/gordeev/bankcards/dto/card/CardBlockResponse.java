package com.gordeev.bankcards.dto.card;

import com.gordeev.bankcards.enums.BlockRequestStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CardBlockResponse(
        Long id,
        UUID userId,
        Long cardId,
        String maskedNumber,
        String reason,
        String rejectReason,
        BlockRequestStatus status,
        OffsetDateTime requestedAt,
        OffsetDateTime processedAt
) {
}
