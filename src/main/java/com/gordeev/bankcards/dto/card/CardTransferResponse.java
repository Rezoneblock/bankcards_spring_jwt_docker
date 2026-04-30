package com.gordeev.bankcards.dto.card;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CardTransferResponse(
        Long fromCardId,
        String fromCardMasked,
        Long toCardId,
        String toCardMasked,
        BigDecimal amount,
        BigDecimal fromCardNewBalance,
        BigDecimal toCardNewBalance,
        OffsetDateTime transferredAt
) {
}
