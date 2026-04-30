package com.gordeev.bankcards.repository;

import com.gordeev.bankcards.entity.CardBlock;
import com.gordeev.bankcards.enums.BlockRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CardBlockRepository extends JpaRepository<CardBlock, Long> {
    Page<CardBlock> findAllByUserId(UUID userId, Pageable pageable);

    Page<CardBlock> findAllByStatus(BlockRequestStatus status, Pageable pageable);

    boolean existsByCardIdAndStatus(Long cardId, BlockRequestStatus status);
}
