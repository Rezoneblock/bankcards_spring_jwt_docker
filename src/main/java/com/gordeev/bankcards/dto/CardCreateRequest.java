package com.gordeev.bankcards.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CardCreateRequest(
        @NotNull UUID userId,
        @NotBlank @Size(max = 150) String ownerName,
        @NotNull LocalDate expirationDate
        ) {
}
