package com.gordeev.bankcards.service;

import com.gordeev.bankcards.dto.api.PageResponse;
import com.gordeev.bankcards.dto.card.CardCreateRequest;
import com.gordeev.bankcards.dto.card.CardResponse;
import com.gordeev.bankcards.entity.Card;
import com.gordeev.bankcards.entity.User;
import com.gordeev.bankcards.exception.ResourceAlreadyExistException;
import com.gordeev.bankcards.exception.ResourceDoesNotExistException;
import com.gordeev.bankcards.mapper.CardMapper;
import com.gordeev.bankcards.repository.CardRepository;
import com.gordeev.bankcards.repository.UserRepository;
import com.gordeev.bankcards.util.CardEncryptionService;
import com.gordeev.bankcards.util.CardHashSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CardService {
    public static final String CARD_NOT_FOUND = "Карты не существует";
    public static final String CARD_ALREADY_EXISTS = "Карта уже существует";

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;

    private final CardEncryptionService cardEncryptionService;
    private final CardHashSearchService cardHashSearchService;

    @Transactional(readOnly = true)
    public PageResponse<CardResponse> getCards(UUID userId, Pageable pageable) {
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
}
