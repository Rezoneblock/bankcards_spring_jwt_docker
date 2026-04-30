package com.example.bankcards.service;

import com.gordeev.bankcards.dto.card.CardCreateRequest;
import com.gordeev.bankcards.dto.card.CardResponse;
import com.gordeev.bankcards.dto.card.CardTransferRequest;
import com.gordeev.bankcards.dto.card.CardTransferResponse;
import com.gordeev.bankcards.entity.Card;
import com.gordeev.bankcards.entity.User;
import com.gordeev.bankcards.enums.CardStatus;
import com.gordeev.bankcards.exception.ResourceAlreadyExistException;
import com.gordeev.bankcards.exception.ResourceDoesNotExistException;
import com.gordeev.bankcards.mapper.CardMapper;
import com.gordeev.bankcards.repository.CardRepository;
import com.gordeev.bankcards.repository.UserRepository;
import com.gordeev.bankcards.service.CardService;
import com.gordeev.bankcards.service.UserService;
import com.gordeev.bankcards.util.CardEncryptionService;
import com.gordeev.bankcards.util.CardHashSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardService тесты")
public class CardServiceTest {
    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private CardEncryptionService encryptionService;

    @Mock
    private CardHashSearchService hashSearchService;

    @InjectMocks
    private CardService cardService;

    @Test
    @DisplayName("Успешное создание карты")
    void createCard_Success() {
        UUID userId = UUID.randomUUID();

        CardCreateRequest request = new CardCreateRequest(
                "1234567812345678",
                "TEST USER",
                new BigDecimal("1000.00"),
                userId
        );

        User user = new User();
        user.setId(userId);
        user.setEnabled(true);

        Card card = new Card();
        card.setId(1L);
        card.setUser(user);
        card.setBalance(new BigDecimal("1000.00"));
        card.setStatus(CardStatus.ACTIVE);
        card.setLastFourDigits("5678");

        CardResponse expectedResponse = new CardResponse(
                1L,
                "**** **** **** 5678",
                "TEST USER",
                LocalDate.now().plusYears(5),
                null,
                new BigDecimal("1000.00"),
                "ACTIVE",
                userId,
                null,
                null
        );

        String cardHash = "hash123";
        String encryptedNumber = "encrypted456";

        // моки
        when(hashSearchService.generateHash(anyString())).thenReturn(cardHash);
        when(cardRepository.existsByCardHashNumber(cardHash)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardMapper.toCard(request)).thenReturn(card);
        when(encryptionService.encryptCardNumber(anyString())).thenReturn(encryptedNumber);
        when(cardRepository.save(any(Card.class))).thenReturn(card);
        when(cardMapper.toResponse(card)).thenReturn(expectedResponse);

        // вызов метода
        CardResponse result = cardService.createCard(request);

        // проверки
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.maskedNumber()).isEqualTo("**** **** **** 5678");

        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("Ошибка при создании карты - карта уже существует")
    void createCard_DuplicateCard_ThrowsException() {

        UUID userId = UUID.randomUUID();

        CardCreateRequest request = new CardCreateRequest(
                "1234567812345678",
                "TEST USER",
                new BigDecimal("1000.00"),
                userId
        );

        String cardHash = "hash123";

        // моки
        when(hashSearchService.generateHash(anyString())).thenReturn(cardHash);
        when(cardRepository.existsByCardHashNumber(cardHash)).thenReturn(true);

        // выброс исключение
        assertThatThrownBy(() -> cardService.createCard(request))
                .isInstanceOf(ResourceAlreadyExistException.class)
                .hasMessageContaining(CardService.CARD_ALREADY_EXISTS);

        // проверка что save НЕ вызывался
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    @DisplayName("Ошибка при создании карты - пользователь не найден")
    void createCard_UserNotFound_ThrowsException() {

        UUID userId = UUID.randomUUID();

        CardCreateRequest request = new CardCreateRequest(
                "1234567812345678",
                "TEST USER",
                new BigDecimal("1000.00"),
                userId
        );

        String cardHash = "hash123";

        // моки
        when(hashSearchService.generateHash(anyString())).thenReturn(cardHash);
        when(cardRepository.existsByCardHashNumber(cardHash)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // проверка исключения
        assertThatThrownBy(() -> cardService.createCard(request))
                .isInstanceOf(ResourceDoesNotExistException.class)
                .hasMessageContaining(UserService.USER_NOT_FOUND);

        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    @DisplayName("Ошибка при создании карты - пользователь заблокирован")
    void createCard_UserBlocked_ThrowsException() {
        UUID userId = UUID.randomUUID();
        CardCreateRequest request = new CardCreateRequest(
                "1234567812345678", "TEST USER", new BigDecimal("1000.00"), userId
        );

        User user = new User();
        user.setId(userId);
        user.setEnabled(false); // ← Пользователь заблокирован

        when(hashSearchService.generateHash(anyString())).thenReturn("hash");
        when(cardRepository.existsByCardHashNumber(anyString())).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> cardService.createCard(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Нельзя создать карту для заблокированного пользователя");
    }

    @Test
    @DisplayName("Успешный перевод между картами")
    void transferBetweenCards_Success() {
        UUID userId = UUID.randomUUID();
        CardTransferRequest request = new CardTransferRequest(1L, 2L, new BigDecimal("500.00"));

        User user = new User();
        user.setId(userId);
        user.setEnabled(true);

        Card fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setUser(user);
        fromCard.setBalance(new BigDecimal("1000.00"));
        fromCard.setLastFourDigits("3456");
        fromCard.setStatus(CardStatus.ACTIVE);
        fromCard.setExpirationDate(LocalDate.now().plusYears(5));

        Card toCard = new Card();
        toCard.setId(2L);
        toCard.setUser(user);
        toCard.setBalance(new BigDecimal("500.00"));
        toCard.setLastFourDigits("1234");
        toCard.setStatus(CardStatus.ACTIVE);
        toCard.setExpirationDate(LocalDate.now().plusYears(5));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        CardTransferResponse result = cardService.transferBetweenCards(userId, request);

        assertThat(result).isNotNull();
        assertThat(result.amount()).isEqualTo(new BigDecimal("500.00"));
        assertThat(result.fromCardNewBalance()).isEqualTo(new BigDecimal("500.00"));
        assertThat(result.toCardNewBalance()).isEqualTo(new BigDecimal("1000.00"));
        verify(cardRepository, times(2)).save(any(Card.class));
    }

    @Test
    @DisplayName("Ошибка при переводе - недостаточно средств")
    void transferBetweenCards_InsufficientFunds_ThrowsException() {
        UUID userId = UUID.randomUUID();
        CardTransferRequest request = new CardTransferRequest(1L, 2L, new BigDecimal("2000.00"));

        User user = new User();
        user.setId(userId);
        user.setEnabled(true);

        Card fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setUser(user);
        fromCard.setBalance(new BigDecimal("1000.00")); // Всего 1000
        fromCard.setStatus(CardStatus.ACTIVE);
        fromCard.setExpirationDate(LocalDate.now().plusYears(5));

        Card toCard = new Card();
        toCard.setId(2L);
        toCard.setUser(user);
        toCard.setBalance(new BigDecimal("500.00"));
        toCard.setStatus(CardStatus.ACTIVE);
        toCard.setExpirationDate(LocalDate.now().plusYears(5));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> cardService.transferBetweenCards(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Недостаточно средств");
    }

    @Test
    @DisplayName("Ошибка при переводе - перевод на ту же карту")
    void transferBetweenCards_SameCard_ThrowsException() {
        UUID userId = UUID.randomUUID();
        CardTransferRequest request = new CardTransferRequest(1L, 1L, new BigDecimal("500.00"));

        User user = new User();
        user.setId(userId);
        user.setEnabled(true);

        Card card = new Card();
        card.setId(1L);
        card.setUser(user);
        card.setBalance(new BigDecimal("1000.00"));
        card.setStatus(CardStatus.ACTIVE);
        card.setExpirationDate(LocalDate.now().plusYears(5));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardService.transferBetweenCards(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Нельзя перевести деньги на ту же самую карту");
    }

    @Test
    @DisplayName("Ошибка при переводе - карта отправителя заблокирована")
    void transferBetweenCards_FromCardBlocked_ThrowsException() {
        UUID userId = UUID.randomUUID();
        CardTransferRequest request = new CardTransferRequest(1L, 2L, new BigDecimal("500.00"));

        User user = new User();
        user.setId(userId);
        user.setEnabled(true);

        Card fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setUser(user);
        fromCard.setBalance(new BigDecimal("1000.00"));
        fromCard.setStatus(CardStatus.BLOCKED); // ← Карта заблокирована
        fromCard.setExpirationDate(LocalDate.now().plusYears(5));

        Card toCard = new Card();
        toCard.setId(2L);
        toCard.setUser(user);
        toCard.setBalance(new BigDecimal("500.00"));
        toCard.setStatus(CardStatus.ACTIVE);
        toCard.setExpirationDate(LocalDate.now().plusYears(5));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThatThrownBy(() -> cardService.transferBetweenCards(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Карта отправителя заблокирована");
    }

    @Test
    @DisplayName("Получение конкретной карты по ID")
    void getCards_ByCardId_Success() {
        UUID userId = UUID.randomUUID();
        Long cardId = 1L;

        Card card = new Card();
        card.setId(cardId);
        User user = new User();
        user.setId(userId);
        card.setUser(user);

        CardResponse expectedResponse = new CardResponse(
                cardId, "**** **** **** 5678", "TEST USER",
                LocalDate.now().plusYears(5), null, new BigDecimal("1000.00"),
                "ACTIVE", userId, null, null
        );

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(card)).thenReturn(expectedResponse);

        var result = cardService.getCards(userId, null, cardId);

        assertThat(result).isNotNull();
        assertThat(result.entities()).hasSize(1);
        assertThat(result.entities().get(0).id()).isEqualTo(cardId);
    }

    @Test
    @DisplayName("Ошибка при получении чужой карты")
    void getCards_OtherUsersCard_ThrowsException() {
        UUID currentUserId = UUID.randomUUID();
        UUID cardOwnerId = UUID.randomUUID();
        Long cardId = 1L;

        Card card = new Card();
        card.setId(cardId);
        User owner = new User();
        owner.setId(cardOwnerId);
        card.setUser(owner);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardService.getCards(currentUserId, null, cardId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Это не ваша карта");
    }
}
