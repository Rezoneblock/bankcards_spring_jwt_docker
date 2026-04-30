package com.gordeev.bankcards.controller.user;

import com.gordeev.bankcards.dto.api.ApiResponse;
import com.gordeev.bankcards.dto.card.CardBlockRequest;
import com.gordeev.bankcards.dto.card.CardBlockResponse;
import com.gordeev.bankcards.security.CustomUserDetails;
import com.gordeev.bankcards.service.CardBlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user/block")
@RequiredArgsConstructor
public class UserCardBlockController {
    private final CardBlockService cardBlockService;

    @PostMapping
    public ResponseEntity<ApiResponse<CardBlockResponse>> createBlockRequest(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody @Valid CardBlockRequest request
    ) {
        CardBlockResponse cardBlockResponse = cardBlockService.createBlockRequest(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(cardBlockResponse));
    }
}
