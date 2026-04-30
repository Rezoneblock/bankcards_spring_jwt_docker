package com.gordeev.bankcards.service;

import com.gordeev.bankcards.dto.card.CardBlockRequest;
import com.gordeev.bankcards.dto.card.CardBlockResponse;
import com.gordeev.bankcards.entity.Card;
import com.gordeev.bankcards.entity.CardBlock;
import com.gordeev.bankcards.enums.BlockRequestStatus;
import com.gordeev.bankcards.enums.CardStatus;
import com.gordeev.bankcards.mapper.CardBlockMapper;
import com.gordeev.bankcards.repository.CardBlockRepository;
import com.gordeev.bankcards.repository.CardRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CardBlockService {
    private final CardBlockRepository cardBlockRepository;
    private final CardRepository cardRepository;
    private final CardBlockMapper cardBlockMapper;

    public CardBlockResponse createBlockRequest(UUID userId, CardBlockRequest request) {
        Card card = cardRepository.findById(request.cardId())
                .orElseThrow(() -> new EntityNotFoundException(CardService.CARD_NOT_FOUND));

        if (!card.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Это не ваша карта");
        }

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new IllegalStateException("Карта уже заблокирована");
        }

        boolean exists = cardBlockRepository.existsByCardIdAndStatus(
                card.getId(), BlockRequestStatus.PENDING
        );
        if (exists) {
            throw new IllegalStateException("Запрос уже отправлен");
        }

        CardBlock cardBlock = CardBlock.builder()
                .card(card)
                .user(card.getUser())
                .reason(request.reason())
                .status(BlockRequestStatus.PENDING)
                .build();

        return cardBlockMapper.toResponse(cardBlockRepository.save(cardBlock));
    }
}
