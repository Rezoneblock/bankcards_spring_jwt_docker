package com.gordeev.bankcards.dto.card.block;

import com.gordeev.bankcards.enums.BlockRequestStatus;
import jakarta.validation.constraints.NotNull;

public record CardBlockDecisionRequest(
        @NotNull(message = "ID заявки обязателен")
        Long requestId,
        @NotNull(message = "Решение (одобрение/отказ) заявки обязательно")
        BlockRequestStatus action,

        String rejectReason
) {
}
