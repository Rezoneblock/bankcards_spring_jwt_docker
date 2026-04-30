package com.gordeev.bankcards.controller.admin;

import com.gordeev.bankcards.dto.api.ApiResponse;
import com.gordeev.bankcards.dto.api.PageResponse;
import com.gordeev.bankcards.dto.card.CardCreateRequest;
import com.gordeev.bankcards.dto.card.CardCreateResponse;
import com.gordeev.bankcards.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/cards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final CardService cardService;

    // Поиск карт с пагинацией, можно вытащить только карты конкретного пользователя via UUID
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CardCreateResponse>>> getCards(
            @RequestParam(required = false) UUID userId,
            @PageableDefault(size = 3, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
            ) {
        PageResponse<CardCreateResponse> result = cardService.getCards(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CardCreateResponse>> createCard(@RequestBody @Valid CardCreateRequest request) {
        CardCreateResponse result = cardService.createCard(request);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
