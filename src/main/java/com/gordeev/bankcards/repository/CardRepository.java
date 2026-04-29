package com.gordeev.bankcards.repository;

import com.gordeev.bankcards.entity.Card;
import com.gordeev.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card,Long> {
    Page<Card> findByUser(User user, Pageable pageable);
    Optional<Card> findByIdAndUser(Long cardId, User user);
    boolean existsByCardNumber(String encryptedCardNumber);
    Optional<Card> findByCardNumber(String encryptedCardNumber);
}
