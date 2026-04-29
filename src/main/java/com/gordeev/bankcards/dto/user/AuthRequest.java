package com.gordeev.bankcards.dto.user;

public record AuthRequest(
        String username,
        String password
) {
}
