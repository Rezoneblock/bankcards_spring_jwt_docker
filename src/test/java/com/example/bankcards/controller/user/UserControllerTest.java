package com.gordeev.bankcards.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordeev.bankcards.dto.api.PageResponse;
import com.gordeev.bankcards.dto.card.CardResponse;
import com.gordeev.bankcards.dto.card.CardTransferRequest;
import com.gordeev.bankcards.dto.card.CardTransferResponse;
import com.gordeev.bankcards.dto.card.block.CardBlockRequest;
import com.gordeev.bankcards.dto.card.block.CardBlockResponse;
import com.gordeev.bankcards.enums.BlockRequestStatus;
import com.gordeev.bankcards.security.userDetails.CustomUserDetails;
import com.gordeev.bankcards.service.CardBlockService;
import com.gordeev.bankcards.service.CardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
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
@DisplayName("UserController MockMvc Тесты")
class UserControllerTest {

    @Mock
    private CardService cardService;

    @Mock
    private CardBlockService cardBlockService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID userId;
    private Long cardId;
    private Long requestId;
    private CardResponse cardResponse;
    private CardBlockResponse cardBlockResponse;
    private CardTransferResponse cardTransferResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        cardId = 1L;
        requestId = 1L;

        // Настройка SecurityContext для @AuthenticationPrincipal
        CustomUserDetails customUserDetails = mock(CustomUserDetails.class);
        lenient().when(customUserDetails.getId()).thenReturn(userId);
        lenient().when(authentication.getPrincipal()).thenReturn(customUserDetails);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver()
                )
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

        cardTransferResponse = new CardTransferResponse(
                cardId,
                "**** **** **** 3456",
                2L,
                "**** **** **** 1234",
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("1500.00"),
                OffsetDateTime.now()
        );
    }

    // Кастомный ArgumentResolver для @AuthenticationPrincipal
    static class AuthenticationPrincipalArgumentResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        }
    }

    // ==================== GET CARDS TESTS ====================

    @Test
    @DisplayName("GET /api/v1/user - успешное получение всех карт пользователя")
    void getCards_Success() throws Exception {
        PageResponse<CardResponse> pageResponse = new PageResponse<>(
                List.of(cardResponse),
                new PageResponse.Metadata(10, 1, 1, 0)
        );

        when(cardService.getCards(eq(userId), any(), isNull())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/user")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.entities[0].id").value(cardId));
    }

    @Test
    @DisplayName("GET /api/v1/user - получение конкретной карты по cardId")
    void getCards_FilterByCardId() throws Exception {
        PageResponse<CardResponse> pageResponse = new PageResponse<>(
                List.of(cardResponse),
                new PageResponse.Metadata(1, 1, 1, 0)
        );

        when(cardService.getCards(eq(userId), any(), eq(cardId))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/user")
                        .param("cardId", cardId.toString())
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.entities[0].id").value(cardId));
    }

    @Test
    @DisplayName("GET /api/v1/user - с пагинацией")
    void getCards_WithPagination() throws Exception {
        PageResponse<CardResponse> pageResponse = new PageResponse<>(
                List.of(cardResponse),
                new PageResponse.Metadata(5, 10, 2, 1)
        );

        when(cardService.getCards(eq(userId), any(), isNull())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/user")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "id,desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.meta.currentPageNumber").value(1));
    }

    // ==================== CREATE BLOCK REQUEST TESTS ====================

    @Test
    @DisplayName("POST /api/v1/user/block - успешное создание заявки на блокировку")
    void createBlockRequest_Success() throws Exception {
        CardBlockRequest request = new CardBlockRequest(cardId, "Card lost");

        when(cardBlockService.createBlockRequest(eq(userId), any(CardBlockRequest.class)))
                .thenReturn(cardBlockResponse);

        mockMvc.perform(post("/api/v1/user/block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(requestId))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/user/block - ошибка валидации (отсутствует cardId)")
    void createBlockRequest_ValidationError_MissingCardId() throws Exception {
        CardBlockRequest invalidRequest = new CardBlockRequest(null, "Card lost");

        mockMvc.perform(post("/api/v1/user/block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // ==================== TRANSFER TESTS ====================

    @Test
    @DisplayName("POST /api/v1/user/transfer - успешный перевод между картами")
    void transferBetweenCards_Success() throws Exception {
        CardTransferRequest request = new CardTransferRequest(cardId, 2L, new BigDecimal("500.00"));

        when(cardService.transferBetweenCards(eq(userId), any(CardTransferRequest.class)))
                .thenReturn(cardTransferResponse);

        mockMvc.perform(post("/api/v1/user/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fromCardId").value(cardId))
                .andExpect(jsonPath("$.data.toCardId").value(2L))
                .andExpect(jsonPath("$.data.amount").value(500.00));
    }

    @Test
    @DisplayName("POST /api/v1/user/transfer - ошибка валидации (отсутствует fromCardId)")
    void transferBetweenCards_ValidationError_MissingFromCardId() throws Exception {
        CardTransferRequest invalidRequest = new CardTransferRequest(null, 2L, new BigDecimal("500.00"));

        mockMvc.perform(post("/api/v1/user/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/user/transfer - ошибка валидации (отсутствует toCardId)")
    void transferBetweenCards_ValidationError_MissingToCardId() throws Exception {
        CardTransferRequest invalidRequest = new CardTransferRequest(cardId, null, new BigDecimal("500.00"));

        mockMvc.perform(post("/api/v1/user/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/user/transfer - ошибка валидации (отсутствует amount)")
    void transferBetweenCards_ValidationError_MissingAmount() throws Exception {
        CardTransferRequest invalidRequest = new CardTransferRequest(cardId, 2L, null);

        mockMvc.perform(post("/api/v1/user/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/user/transfer - ошибка валидации (отрицательная сумма)")
    void transferBetweenCards_ValidationError_NegativeAmount() throws Exception {
        CardTransferRequest invalidRequest = new CardTransferRequest(cardId, 2L, new BigDecimal("-100.00"));

        mockMvc.perform(post("/api/v1/user/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/user/transfer - ошибка валидации (нулевая сумма)")
    void transferBetweenCards_ValidationError_ZeroAmount() throws Exception {
        CardTransferRequest invalidRequest = new CardTransferRequest(cardId, 2L, BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/user/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}