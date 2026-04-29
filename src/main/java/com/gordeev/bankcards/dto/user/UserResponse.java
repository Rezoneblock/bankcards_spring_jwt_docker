package com.gordeev.bankcards.dto.user;

import com.gordeev.bankcards.entity.Role;
import lombok.Builder;

import java.util.Set;
import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String username,
        String email,
        boolean enabled,
        Set<Role> roles
) {
}
