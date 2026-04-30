package com.gordeev.bankcards.service;

import com.gordeev.bankcards.dto.api.PageResponse;
import com.gordeev.bankcards.dto.card.block.CardBlockDecisionRequest;
import com.gordeev.bankcards.dto.card.block.CardBlockRequest;
import com.gordeev.bankcards.dto.card.block.CardBlockResponse;
import com.gordeev.bankcards.entity.Card;
import com.gordeev.bankcards.entity.CardBlock;
import com.gordeev.bankcards.enums.BlockRequestStatus;
import com.gordeev.bankcards.enums.CardStatus;
import com.gordeev.bankcards.mapper.CardBlockMapper;
import com.gordeev.bankcards.repository.CardBlockRepository;
import com.gordeev.bankcards.repository.CardRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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

    @Transactional(readOnly = true)
    public PageResponse<CardBlockResponse> getCardBlocks(BlockRequestStatus status, Pageable pageable) {
        Page<CardBlock> page;

        if (status != null) {
            page = cardBlockRepository.findByStatus(status, pageable);
        } else {
            page = cardBlockRepository.findAll(pageable);
        }

        Page<CardBlockResponse> responsePage = page.map(cardBlockMapper::toResponse);

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

    public CardBlockResponse blockRequestDecision(UUID adminId, CardBlockDecisionRequest request) {
        CardBlock blockRequest = cardBlockRepository.findById(request.requestId())
                .orElseThrow(() -> new EntityNotFoundException("Заявка на блокировку не найдена"));

        if (blockRequest.getStatus() != BlockRequestStatus.PENDING) {
            throw new IllegalStateException("Заявка уже обработана. Текущий статус: " + blockRequest.getStatus());
        }

        if (request.action() == BlockRequestStatus.APPROVED) {
            Card card = blockRequest.getCard();

            if (card.getStatus() == CardStatus.BLOCKED) {
                throw new IllegalStateException("Карта уже заблокирована");
            }

            card.setStatus(CardStatus.BLOCKED);
            card.setBlockReason(blockRequest.getReason());
            card.setBlockedAt(OffsetDateTime.now());
            card.setBlockedBy(adminId);
            cardRepository.save(card);

            blockRequest.setStatus(BlockRequestStatus.APPROVED);


        } else if (request.action() == BlockRequestStatus.REJECTED) {
            if (request.rejectReason() == null || request.rejectReason().isBlank()) {
                throw new IllegalArgumentException("Причина отклонения обязательна при отклонении заявки");
            }
            blockRequest.setRejectReason(request.rejectReason());
            blockRequest.setStatus(BlockRequestStatus.REJECTED);
        } else {
            throw new IllegalArgumentException("Некорректное действие. Используйте APPROVED или REJECTED");
        }

        blockRequest.setProcessedAt(OffsetDateTime.now());
        blockRequest.setProcessedBy(adminId);

        return cardBlockMapper.toResponse(cardBlockRepository.save(blockRequest));
    }
}
