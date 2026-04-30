package com.gordeev.bankcards.repository;

import com.gordeev.bankcards.entity.Card;
import com.gordeev.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card,Long> {
    Page<Card> findByUserId(UUID id, Pageable pageable);
    Optional<Card> findByIdAndUser(Long cardId, User user);
    boolean existsByCardNumber(String encryptedCardNumber);
    boolean existsByCardHashNumber(String cardHash);
    Optional<Card> findByCardNumber(String encryptedCardNumber);
}
