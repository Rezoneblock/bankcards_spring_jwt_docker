package com.gordeev.bankcards.controller.admin;

import com.gordeev.bankcards.dto.api.ApiResponse;
import com.gordeev.bankcards.dto.api.PageResponse;
import com.gordeev.bankcards.dto.card.*;
import com.gordeev.bankcards.dto.card.block.CardBlockDecisionRequest;
import com.gordeev.bankcards.dto.card.block.CardBlockResponse;
import com.gordeev.bankcards.dto.user.UserResponse;
import com.gordeev.bankcards.dto.user.UserStatusUpdateRequest;
import com.gordeev.bankcards.enums.BlockRequestStatus;
import com.gordeev.bankcards.security.userDetails.CustomUserDetails;
import com.gordeev.bankcards.service.CardBlockService;
import com.gordeev.bankcards.service.CardService;
import com.gordeev.bankcards.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final CardService cardService;
    private final UserService userService;
    private final CardBlockService cardBlockService;

    // Поиск всех карт с пагинацией, ещё можно вытащить карты конкретного пользователя via UUID
    @GetMapping("/cards")
    public ResponseEntity<ApiResponse<PageResponse<CardResponse>>> getCards(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Long cardId,
            @PageableDefault(size = 3, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
            ) {
        PageResponse<CardResponse> result = cardService.getCards(userId, pageable, cardId);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // Создание карты
    @PostMapping("/cards")
    public ResponseEntity<ApiResponse<CardResponse>> createCard(@RequestBody @Valid CardCreateRequest request) {
        CardResponse result = cardService.createCard(request);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // Просмотр заявок на блокировку карт
    @GetMapping("/cards/block")
    public ResponseEntity<ApiResponse<PageResponse<CardBlockResponse>>> getBlocks(
            @RequestParam(required = false) BlockRequestStatus status,
            @PageableDefault(size = 3, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
            ) {
        PageResponse<CardBlockResponse> result = cardBlockService.getCardBlocks(status, pageable);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // Обработка заявок на блокировку карт
    @PatchMapping("/cards/block/card")
    public ResponseEntity<ApiResponse<CardBlockResponse>> blockDecision(
            @RequestBody @Valid CardBlockDecisionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentAdmin
    ) {
        CardBlockResponse result = cardBlockService.blockRequestDecision(currentAdmin.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/cards/unblock")
    public ResponseEntity<ApiResponse<CardResponse>> unblockCard(@RequestParam Long cardId) {
        CardResponse result = cardBlockService.unblockCard(cardId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PatchMapping("/users/block")
    public ResponseEntity<ApiResponse<UserResponse>> userBlock(
            @AuthenticationPrincipal CustomUserDetails currentAdmin,
            @RequestBody @Valid UserStatusUpdateRequest request
    ) {
        UserResponse result = userService.updateUserStatus(currentAdmin.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @PageableDefault(size = 3, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<UserResponse> result = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/cards/delete")
    public ResponseEntity<ApiResponse<String>> softDeleteCard(@RequestParam Long cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.ok(ApiResponse.success("Карта успешно удалена"));
    }
}
