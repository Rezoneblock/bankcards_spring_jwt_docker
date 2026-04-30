package com.example.bankcards.service;

import com.gordeev.bankcards.dto.card.block.CardBlockDecisionRequest;
import com.gordeev.bankcards.dto.card.block.CardBlockRequest;
import com.gordeev.bankcards.dto.card.block.CardBlockResponse;
import com.gordeev.bankcards.entity.Card;
import com.gordeev.bankcards.entity.CardBlock;
import com.gordeev.bankcards.entity.User;
import com.gordeev.bankcards.enums.BlockRequestStatus;
import com.gordeev.bankcards.enums.CardStatus;
import com.gordeev.bankcards.exception.ResourceDoesNotExistException;
import com.gordeev.bankcards.mapper.CardBlockMapper;
import com.gordeev.bankcards.mapper.CardMapper;
import com.gordeev.bankcards.repository.CardBlockRepository;
import com.gordeev.bankcards.repository.CardRepository;
import com.gordeev.bankcards.repository.UserRepository;
import com.gordeev.bankcards.service.CardBlockService;
import com.gordeev.bankcards.service.CardService;
import com.gordeev.bankcards.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardBlockService тесты")
class CardBlockServiceTest {

    @Mock
    private CardBlockRepository cardBlockRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardBlockMapper cardBlockMapper;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private CardBlockService cardBlockService;

    private UUID userId;
    private UUID adminId;
    private Long cardId;
    private Long requestId;
    private User user;
    private Card card;
    private CardBlock cardBlock;
    private CardBlockResponse cardBlockResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        cardId = 1L;
        requestId = 1L;

        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEnabled(true);

        card = new Card();
        card.setId(cardId);
        card.setUser(user);
        card.setStatus(CardStatus.ACTIVE);
        card.setLastFourDigits("3456");
        card.setBalance(new java.math.BigDecimal("1000.00"));
        card.setExpirationDate(LocalDate.now().plusYears(5));

        cardBlock = CardBlock.builder()
                .id(requestId)
                .card(card)
                .user(user)
                .reason("Card lost")
                .status(BlockRequestStatus.PENDING)
                .build();

        cardBlockResponse = new CardBlockResponse(
                requestId, userId, cardId, "**** **** **** 3456",
                "Card lost", null, BlockRequestStatus.PENDING, null, null
        );
    }

    // Crate block requests

    @Test
    @DisplayName("Успешное создание заявки на блокировку")
    void createBlockRequest_Success() {
        CardBlockRequest request = new CardBlockRequest(cardId, "Card lost");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardBlockRepository.existsByCardIdAndStatus(cardId, BlockRequestStatus.PENDING))
                .thenReturn(false);
        when(cardBlockRepository.save(any(CardBlock.class))).thenReturn(cardBlock);
        when(cardBlockMapper.toResponse(cardBlock)).thenReturn(cardBlockResponse);

        CardBlockResponse result = cardBlockService.createBlockRequest(userId, request);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(BlockRequestStatus.PENDING);
        assertThat(result.cardId()).isEqualTo(cardId);
        verify(cardBlockRepository).save(any(CardBlock.class));
    }

    @Test
    @DisplayName("Ошибка при создании заявки - пользователь не найден")
    void createBlockRequest_UserNotFound_ThrowsException() {
        CardBlockRequest request = new CardBlockRequest(cardId, "Card lost");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardBlockService.createBlockRequest(userId, request))
                .isInstanceOf(ResourceDoesNotExistException.class)
                .hasMessageContaining(UserService.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Ошибка при создании заявки - пользователь заблокирован")
    void createBlockRequest_UserBlocked_ThrowsException() {
        user.setEnabled(false);
        CardBlockRequest request = new CardBlockRequest(cardId, "Card lost");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> cardBlockService.createBlockRequest(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Заблокированный пользователь не может блокировать карту");
    }

    @Test
    @DisplayName("Ошибка при создании заявки - карта не найдена")
    void createBlockRequest_CardNotFound_ThrowsException() {
        CardBlockRequest request = new CardBlockRequest(cardId, "Card lost");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardBlockService.createBlockRequest(userId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(CardService.CARD_NOT_FOUND);
    }

    @Test
    @DisplayName("Ошибка при создании заявки - попытка заблокировать чужую карту")
    void createBlockRequest_NotOwnCard_ThrowsException() {
        UUID anotherUserId = UUID.randomUUID();
        CardBlockRequest request = new CardBlockRequest(cardId, "Card lost");

        when(userRepository.findById(anotherUserId)).thenReturn(Optional.of(user));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardBlockService.createBlockRequest(anotherUserId, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Это не ваша карта");
    }

    @Test
    @DisplayName("Ошибка при создании заявки - карта уже заблокирована")
    void createBlockRequest_CardAlreadyBlocked_ThrowsException() {
        card.setStatus(CardStatus.BLOCKED);
        CardBlockRequest request = new CardBlockRequest(cardId, "Card lost");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardBlockService.createBlockRequest(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Карта уже заблокирована");
    }

    @Test
    @DisplayName("Ошибка при создании заявки - запрос уже отправлен")
    void createBlockRequest_AlreadyPending_ThrowsException() {
        CardBlockRequest request = new CardBlockRequest(cardId, "Card lost");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardBlockRepository.existsByCardIdAndStatus(cardId, BlockRequestStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> cardBlockService.createBlockRequest(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Запрос уже отправлен");
    }

    // Block request decisions

    @Test
    @DisplayName("Админ одобряет заявку - карта блокируется")
    void blockRequestDecision_Approve_Success() {
        CardBlockDecisionRequest decisionRequest = new CardBlockDecisionRequest(
                requestId, BlockRequestStatus.APPROVED, null
        );

        when(cardBlockRepository.findById(requestId)).thenReturn(Optional.of(cardBlock));
        when(cardRepository.save(any(Card.class))).thenReturn(card);
        when(cardBlockRepository.save(any(CardBlock.class))).thenReturn(cardBlock);
        when(cardBlockMapper.toResponse(cardBlock)).thenReturn(cardBlockResponse);

        CardBlockResponse result = cardBlockService.blockRequestDecision(adminId, decisionRequest);

        assertThat(result).isNotNull();
        assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);
        assertThat(card.getBlockedBy()).isEqualTo(adminId);
        assertThat(card.getBlockReason()).isEqualTo("Card lost");
        assertThat(cardBlock.getStatus()).isEqualTo(BlockRequestStatus.APPROVED);

        verify(cardRepository).save(card);
        verify(cardBlockRepository).save(cardBlock);
    }

    @Test
    @DisplayName("Админ отклоняет заявку с указанием причины")
    void blockRequestDecision_Reject_Success() {
        String rejectReason = "Not enough evidence";
        CardBlockDecisionRequest decisionRequest = new CardBlockDecisionRequest(
                requestId, BlockRequestStatus.REJECTED, rejectReason
        );

        when(cardBlockRepository.findById(requestId)).thenReturn(Optional.of(cardBlock));
        when(cardBlockRepository.save(any(CardBlock.class))).thenReturn(cardBlock);
        when(cardBlockMapper.toResponse(cardBlock)).thenReturn(cardBlockResponse);

        CardBlockResponse result = cardBlockService.blockRequestDecision(adminId, decisionRequest);

        assertThat(result).isNotNull();
        assertThat(cardBlock.getStatus()).isEqualTo(BlockRequestStatus.REJECTED);
        assertThat(cardBlock.getRejectReason()).isEqualTo(rejectReason);
        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE); // Карта не блокируется

        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    @DisplayName("Ошибка - заявка не найдена")
    void blockRequestDecision_RequestNotFound_ThrowsException() {
        CardBlockDecisionRequest decisionRequest = new CardBlockDecisionRequest(
                requestId, BlockRequestStatus.APPROVED, null
        );

        when(cardBlockRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardBlockService.blockRequestDecision(adminId, decisionRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Заявка на блокировку не найдена");
    }

    @Test
    @DisplayName("Ошибка - заявка уже обработана")
    void blockRequestDecision_AlreadyProcessed_ThrowsException() {
        cardBlock.setStatus(BlockRequestStatus.APPROVED);
        CardBlockDecisionRequest decisionRequest = new CardBlockDecisionRequest(
                requestId, BlockRequestStatus.APPROVED, null
        );

        when(cardBlockRepository.findById(requestId)).thenReturn(Optional.of(cardBlock));

        assertThatThrownBy(() -> cardBlockService.blockRequestDecision(adminId, decisionRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Заявка уже обработана");
    }

    @Test
    @DisplayName("Ошибка - отклонение без причины")
    void blockRequestDecision_RejectWithoutReason_ThrowsException() {
        CardBlockDecisionRequest decisionRequest = new CardBlockDecisionRequest(
                requestId, BlockRequestStatus.REJECTED, null
        );

        when(cardBlockRepository.findById(requestId)).thenReturn(Optional.of(cardBlock));

        assertThatThrownBy(() -> cardBlockService.blockRequestDecision(adminId, decisionRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Причина отклонения обязательна");
    }

    @Test
    @DisplayName("Ошибка - одобрение уже заблокированной карты")
    void blockRequestDecision_ApproveAlreadyBlockedCard_ThrowsException() {
        card.setStatus(CardStatus.BLOCKED);
        CardBlockDecisionRequest decisionRequest = new CardBlockDecisionRequest(
                requestId, BlockRequestStatus.APPROVED, null
        );

        when(cardBlockRepository.findById(requestId)).thenReturn(Optional.of(cardBlock));

        assertThatThrownBy(() -> cardBlockService.blockRequestDecision(adminId, decisionRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Карта уже заблокирована");
    }

    @Test
    @DisplayName("Ошибка - некорректное действие")
    void blockRequestDecision_InvalidAction_ThrowsException() {
        // некорректный action (не APPROVED и не REJECTED)
        CardBlockDecisionRequest decisionRequest = mock(CardBlockDecisionRequest.class);
        when(decisionRequest.requestId()).thenReturn(requestId);
        when(decisionRequest.action()).thenReturn(null);

        when(cardBlockRepository.findById(requestId)).thenReturn(Optional.of(cardBlock));

        assertThatThrownBy(() -> cardBlockService.blockRequestDecision(adminId, decisionRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Некорректное действие");
    }

    // Unblock card

    @Test
    @DisplayName("Успешная разблокировка карты")
    void unblockCard_Success() {
        card.setStatus(CardStatus.BLOCKED);
        card.setBlockReason("Card lost");
        card.setBlockedAt(OffsetDateTime.now());
        card.setBlockedBy(adminId);

        com.gordeev.bankcards.dto.card.CardResponse expectedResponse = new com.gordeev.bankcards.dto.card.CardResponse(
                cardId, "**** **** **** 3456", "TEST USER",
                LocalDate.now().plusYears(5), null, new java.math.BigDecimal("1000.00"),
                "ACTIVE", userId, null, null
        );

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenReturn(card);
        when(cardMapper.toResponse(card)).thenReturn(expectedResponse);

        var result = cardBlockService.unblockCard(cardId);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.blockReason()).isNull();
        verify(cardRepository).save(card);
    }

    @Test
    @DisplayName("Ошибка при разблокировке - карта не найдена")
    void unblockCard_CardNotFound_ThrowsException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardBlockService.unblockCard(cardId))
                .isInstanceOf(ResourceDoesNotExistException.class)
                .hasMessageContaining(CardService.CARD_NOT_FOUND);
    }

    @Test
    @DisplayName("Ошибка при разблокировке - карта не заблокирована")
    void unblockCard_CardNotBlocked_ThrowsException() {
        card.setStatus(CardStatus.ACTIVE);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardBlockService.unblockCard(cardId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Можно разблокировать только заблокированную карту");
    }

    @Test
    @DisplayName("Ошибка при разблокировке - карта просрочена")
    void unblockCard_CardExpired_ThrowsException() {
        card.setStatus(CardStatus.BLOCKED);
        card.setExpirationDate(LocalDate.now().minusDays(1)); // Просрочена

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardBlockService.unblockCard(cardId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Нельзя разблокировать просроченную карту");
    }
}