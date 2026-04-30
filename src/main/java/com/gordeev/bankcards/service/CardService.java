package com.gordeev.bankcards.service;

import com.gordeev.bankcards.dto.api.PageResponse;
import com.gordeev.bankcards.dto.card.CardCreateRequest;
import com.gordeev.bankcards.dto.card.CardResponse;
import com.gordeev.bankcards.dto.card.CardTransferRequest;
import com.gordeev.bankcards.dto.card.CardTransferResponse;
import com.gordeev.bankcards.entity.Card;
import com.gordeev.bankcards.entity.CardBlock;
import com.gordeev.bankcards.entity.User;
import com.gordeev.bankcards.enums.CardStatus;
import com.gordeev.bankcards.exception.ResourceAlreadyExistException;
import com.gordeev.bankcards.exception.ResourceDoesNotExistException;
import com.gordeev.bankcards.mapper.CardMapper;
import com.gordeev.bankcards.repository.CardBlockRepository;
import com.gordeev.bankcards.repository.CardRepository;
import com.gordeev.bankcards.repository.UserRepository;
import com.gordeev.bankcards.util.CardEncryptionService;
import com.gordeev.bankcards.util.CardHashSearchService;
import com.gordeev.bankcards.util.MaskCardNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CardService {
    public static final String CARD_NOT_FOUND = "Карты не существует";
    public static final String CARD_ALREADY_EXISTS = "Карта уже существует";

    private final CardRepository cardRepository;
    private final CardBlockRepository cardBlockRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;

    private final CardEncryptionService cardEncryptionService;
    private final CardHashSearchService cardHashSearchService;

    @Transactional(readOnly = true)
    public PageResponse<CardResponse> getCards(UUID userId, Pageable pageable, Long cardId) {
        if (cardId != null) {
            Card card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new ResourceDoesNotExistException("Карта не найдена"));

            if (userId != null && !card.getUser().getId().equals(userId)) {
                throw new AccessDeniedException("Это не ваша карта");
            }

            CardResponse result = cardMapper.toResponse(card);

            return new PageResponse<>(
                    List.of(result),
                    new PageResponse.Metadata(
                            1,
                            1,
                            1,
                            0
                    )
            );
        }

        Page<Card> page;

        if (userId != null) {
            if (!userRepository.existsById(userId)) {
                throw new ResourceDoesNotExistException(UserService.USER_NOT_FOUND);
            }
            page = cardRepository.findByUserId(userId, pageable);
        } else {
            page = cardRepository.findAll(pageable);
        }

        Page<CardResponse> responsePage = page.map(cardMapper::toResponse);

        return new PageResponse<>(
                responsePage.getContent(),
                new PageResponse.Metadata(
                        responsePage.getSize(),
                        responsePage.getTotalElements(),
                        responsePage.getTotalPages(),
                        responsePage.getNumber()
                )
        );
    }

    public CardResponse createCard(CardCreateRequest request) {

        String cardHash = cardHashSearchService.generateHash(request.cardNumber());

        if (cardRepository.existsByCardHashNumber(cardHash)) {
            throw new ResourceAlreadyExistException(CARD_ALREADY_EXISTS);
        }

        User user = userRepository.findById(request.userId()).orElseThrow(() -> new ResourceDoesNotExistException(UserService.USER_NOT_FOUND));

        if (!user.isEnabled()) {
            throw new IllegalStateException("Нельзя создать карту для заблокированного пользователя");
        }

        Card card = cardMapper.toCard(request);
        card.setUser(user);
        card.setBalance(request.initialBalance());
        card.setCardNumber(cardEncryptionService.encryptCardNumber(request.cardNumber()));
        card.setCardHashNumber(cardHash);

        return cardMapper.toResponse(cardRepository.save(card));
    }

    public CardTransferResponse transferBetweenCards(UUID userId, CardTransferRequest request) {

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceDoesNotExistException(UserService.USER_NOT_FOUND));

        if (!user.isEnabled()) {
            throw new IllegalStateException("Заблокированный пользователь не может переводить деньги");
        }

        Card fromCard = cardRepository.findById(request.fromCardId())
                .orElseThrow(() -> new ResourceDoesNotExistException("Карта отправителя не найдена"));

        Card toCard = cardRepository.findById(request.toCardId())
                .orElseThrow(() -> new ResourceDoesNotExistException("Карта получателя не найдена"));

        if (!fromCard.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Карта отправителя не принадлежит вам");
        }

        if (!toCard.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Карта получателя не принадлежит вам");
        }

        validateCardForTransfer(fromCard, "отправителя");
        validateCardForTransfer(toCard, "получателя");

        if (fromCard.getId().equals(toCard.getId())) {
            throw new IllegalArgumentException("Нельзя перевести деньги на ту же самую карту");
        }

        BigDecimal amount = request.amount();
        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Недостаточно средств. Доступно: " + fromCard.getBalance());
        }

        BigDecimal newFromBalance = fromCard.getBalance().subtract(amount);
        BigDecimal newToBalance = toCard.getBalance().add(amount);

        fromCard.setBalance(newFromBalance);
        toCard.setBalance(newToBalance);

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        return new CardTransferResponse(
                fromCard.getId(),
                MaskCardNumber.mask(fromCard.getLastFourDigits()),
                toCard.getId(),
                MaskCardNumber.mask(toCard.getLastFourDigits()),
                amount,
                newFromBalance,
                newToBalance,
                OffsetDateTime.now()
        );
    }

    private void validateCardForTransfer(Card card, String cardType) {
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new IllegalStateException("Карта " + cardType + " заблокирована");
        }

        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new IllegalStateException("Срок действия карты " + cardType + " истек");
        }

        if (card.getExpirationDate().isBefore(java.time.LocalDate.now())) {
            throw new IllegalStateException("Срок действия карты " + cardType + " истек");
        }
    }

    public void deleteCard (Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceDoesNotExistException(CARD_NOT_FOUND));

        if (card.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Нельзя удалить карту с ненулевым балансом. Баланс: " + card.getBalance());
        }

        if (card.getStatus() == CardStatus.ACTIVE) {
            throw new IllegalStateException("Нельзя удалить активную карту. Сначала заблокируйте её");
        }

        List<CardBlock> blockRequests = cardBlockRepository.findByCardId(cardId);
        if (!blockRequests.isEmpty()) {
            cardBlockRepository.deleteAll(blockRequests);
        }

        cardRepository.delete(card);
    }
}
