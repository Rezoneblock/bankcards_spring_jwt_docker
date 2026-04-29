package com.gordeev.bankcards.dto;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        boolean enabled,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Set<String> roles
) {
}
