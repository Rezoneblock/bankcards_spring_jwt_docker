package com.gordeev.bankcards.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordeev.bankcards.dto.api.PageResponse;
import com.gordeev.bankcards.dto.card.CardCreateRequest;
import com.gordeev.bankcards.dto.card.CardResponse;
import com.gordeev.bankcards.dto.card.block.CardBlockDecisionRequest;
import com.gordeev.bankcards.dto.card.block.CardBlockResponse;
import com.gordeev.bankcards.dto.user.UserResponse;
import com.gordeev.bankcards.dto.user.UserStatusUpdateRequest;
import com.gordeev.bankcards.enums.BlockRequestStatus;
import com.gordeev.bankcards.service.CardBlockService;
import com.gordeev.bankcards.service.CardService;
import com.gordeev.bankcards.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController MockMvc Тесты")
class AdminControllerTest {

    @Mock
    private CardService cardService;

    @Mock
    private UserService userService;

    @Mock
    private CardBlockService cardBlockService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID userId;
    private Long cardId;
    private Long requestId;
    private CardResponse cardResponse;
    private CardBlockResponse cardBlockResponse;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        cardId = 1L;
        requestId = 1L;

        mockMvc = MockMvcBuilders
                .standaloneSetup(adminController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();

        cardResponse = new CardResponse(
                cardId,
                "**** **** **** 3456",
                "TEST USER",
                LocalDate.now().plusYears(5),
                null,
                new BigDecimal("1000.00"),
                "ACTIVE",
                userId,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        cardBlockResponse = new CardBlockResponse(
                requestId,
                userId,
                cardId,
                "**** **** **** 3456",
                "Card lost",
                null,
                BlockRequestStatus.PENDING,
                OffsetDateTime.now(),
                null
        );

        userResponse = UserResponse.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .enabled(true)
                .build();
    }

    // ==================== GET CARDS TESTS ====================

    @Test
    @DisplayName("GET /api/v1/admin/cards - успешное получение всех карт")
    void getCards_Success() throws Exception {
        PageResponse<CardResponse> pageResponse = new PageResponse<>(
                List.of(cardResponse),
                new PageResponse.Metadata(10, 1, 1, 0)
        );

        when(cardService.getCards(any(), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/cards")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.entities[0].id").value(cardId));
    }

    @Test
    @DisplayName("GET /api/v1/admin/cards - фильтрация по userId")
    void getCards_FilterByUserId() throws Exception {
        PageResponse<CardResponse> pageResponse = new PageResponse<>(
                List.of(cardResponse),
                new PageResponse.Metadata(10, 1, 1, 0)
        );

        when(cardService.getCards(eq(userId), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/cards")
                        .param("userId", userId.toString())
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/admin/cards - фильтрация по cardId")
    void getCards_FilterByCardId() throws Exception {
        PageResponse<CardResponse> pageResponse = new PageResponse<>(
                List.of(cardResponse),
                new PageResponse.Metadata(1, 1, 1, 0)
        );

        when(cardService.getCards(isNull(), any(), eq(cardId))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/cards")
                        .param("cardId", cardId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== CREATE CARD TESTS ====================

    @Test
    @DisplayName("POST /api/v1/admin/cards - успешное создание карты")
    void createCard_Success() throws Exception {
        CardCreateRequest request = new CardCreateRequest(
                "1234567812345678",
                "TEST USER",
                new BigDecimal("1000.00"),
                userId
        );

        when(cardService.createCard(any(CardCreateRequest.class))).thenReturn(cardResponse);

        mockMvc.perform(post("/api/v1/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(cardId));
    }

    @Test
    @DisplayName("POST /api/v1/admin/cards - ошибка валидации (неверные данные)")
    void createCard_ValidationError() throws Exception {
        CardCreateRequest invalidRequest = new CardCreateRequest(
                "123",
                "",
                new BigDecimal("-100.00"),
                null
        );

        mockMvc.perform(post("/api/v1/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET BLOCK REQUESTS TESTS ====================

    @Test
    @DisplayName("GET /api/v1/admin/cards/block - успешное получение всех заявок")
    void getBlocks_Success() throws Exception {
        PageResponse<CardBlockResponse> pageResponse = new PageResponse<>(
                List.of(cardBlockResponse),
                new PageResponse.Metadata(10, 1, 1, 0)
        );

        when(cardBlockService.getCardBlocks(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/cards/block")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.entities[0].id").value(requestId));
    }

    @Test
    @DisplayName("GET /api/v1/admin/cards/block - фильтрация по статусу PENDING")
    void getBlocks_FilterByPendingStatus() throws Exception {
        PageResponse<CardBlockResponse> pageResponse = new PageResponse<>(
                List.of(cardBlockResponse),
                new PageResponse.Metadata(10, 1, 1, 0)
        );

        when(cardBlockService.getCardBlocks(eq(BlockRequestStatus.PENDING), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/cards/block")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/admin/cards/block - фильтрация по статусу APPROVED")
    void getBlocks_FilterByApprovedStatus() throws Exception {
        PageResponse<CardBlockResponse> emptyResponse = new PageResponse<>(
                List.of(),
                new PageResponse.Metadata(10, 0, 0, 0)
        );

        when(cardBlockService.getCardBlocks(eq(BlockRequestStatus.APPROVED), any()))
                .thenReturn(emptyResponse);

        mockMvc.perform(get("/api/v1/admin/cards/block")
                        .param("status", "APPROVED")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/admin/cards/block - фильтрация по статусу REJECTED")
    void getBlocks_FilterByRejectedStatus() throws Exception {
        PageResponse<CardBlockResponse> emptyResponse = new PageResponse<>(
                List.of(),
                new PageResponse.Metadata(10, 0, 0, 0)
        );

        when(cardBlockService.getCardBlocks(eq(BlockRequestStatus.REJECTED), any()))
                .thenReturn(emptyResponse);

        mockMvc.perform(get("/api/v1/admin/cards/block")
                        .param("status", "REJECTED")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // ==================== BLOCK DECISION TESTS ====================
    // Примечание: тесты с @AuthenticationPrincipal не включены, так как не работают в standalone режиме

    // ==================== UNBLOCK CARD TESTS ====================

    @Test
    @DisplayName("POST /api/v1/admin/cards/unblock - успешная разблокировка карты")
    void unblockCard_Success() throws Exception {
        when(cardBlockService.unblockCard(eq(cardId))).thenReturn(cardResponse);

        mockMvc.perform(post("/api/v1/admin/cards/unblock")
                        .param("cardId", cardId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(cardId));
    }

    @Test
    @DisplayName("POST /api/v1/admin/cards/unblock - отсутствует параметр cardId")
    void unblockCard_MissingCardId() throws Exception {
        mockMvc.perform(post("/api/v1/admin/cards/unblock")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ==================== USER BLOCK TESTS ====================
    // Примечание: тесты с @AuthenticationPrincipal не включены, так как не работают в standalone режиме

    // ==================== GET USERS TESTS ====================

    @Test
    @DisplayName("GET /api/v1/admin/users - успешное получение всех пользователей")
    void getUsers_Success() throws Exception {
        PageResponse<UserResponse> pageResponse = new PageResponse<>(
                List.of(userResponse),
                new PageResponse.Metadata(10, 1, 1, 0)
        );

        when(userService.getAllUsers(any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.entities[0].username").value("testuser"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - с пагинацией")
    void getUsers_WithPagination() throws Exception {
        PageResponse<UserResponse> pageResponse = new PageResponse<>(
                List.of(userResponse),
                new PageResponse.Metadata(5, 10, 2, 1)
        );

        when(userService.getAllUsers(any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "id,desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entities[0].username").value("testuser"));
    }

    // ==================== DELETE CARD TESTS ====================

    @Test
    @DisplayName("DELETE /api/v1/admin/cards/delete - успешное удаление карты")
    void softDeleteCard_Success() throws Exception {
        doNothing().when(cardService).deleteCard(eq(cardId));

        mockMvc.perform(delete("/api/v1/admin/cards/delete")
                        .param("cardId", cardId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Карта успешно удалена"));

        verify(cardService).deleteCard(eq(cardId));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/cards/delete - отсутствует параметр cardId")
    void softDeleteCard_MissingCardId() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/cards/delete")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}