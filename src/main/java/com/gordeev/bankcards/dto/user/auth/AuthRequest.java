package com.gordeev.bankcards.dto.user.auth;

public record AuthRequest(
        String username,
        String password
) {
}
