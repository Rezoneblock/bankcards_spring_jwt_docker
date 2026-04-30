package com.gordeev.bankcards.repository;

import com.gordeev.bankcards.entity.CardBlock;
import com.gordeev.bankcards.enums.BlockRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardBlockRepository extends JpaRepository<CardBlock, Long> {
    List<CardBlock> findByCardId(Long cardId);

    Page<CardBlock> findByStatus(BlockRequestStatus status, Pageable pageable);

    boolean existsByCardIdAndStatus(Long cardId, BlockRequestStatus status);
}
