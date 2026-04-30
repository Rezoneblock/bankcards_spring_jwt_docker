package com.gordeev.bankcards.controller.user;

import com.gordeev.bankcards.dto.api.ApiResponse;
import com.gordeev.bankcards.dto.api.PageResponse;
import com.gordeev.bankcards.dto.card.CardTransferRequest;
import com.gordeev.bankcards.dto.card.CardTransferResponse;
import com.gordeev.bankcards.dto.card.block.CardBlockRequest;
import com.gordeev.bankcards.dto.card.block.CardBlockResponse;
import com.gordeev.bankcards.dto.card.CardResponse;
import com.gordeev.bankcards.security.userDetails.CustomUserDetails;
import com.gordeev.bankcards.service.CardBlockService;
import com.gordeev.bankcards.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final CardBlockService cardBlockService;
    private final CardService cardService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CardResponse>>> getCards(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) Long cardId,
            @PageableDefault(size = 3, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<CardResponse> result = cardService.getCards(currentUser.getId(), pageable, cardId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/block")
    public ResponseEntity<ApiResponse<CardBlockResponse>> createBlockRequest(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody @Valid CardBlockRequest request
    ) {
        CardBlockResponse cardBlockResponse = cardBlockService.createBlockRequest(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(cardBlockResponse));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<CardTransferResponse>> transferBetweenCards(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody @Valid CardTransferRequest request
    ) {
        CardTransferResponse result = cardService.transferBetweenCards(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
