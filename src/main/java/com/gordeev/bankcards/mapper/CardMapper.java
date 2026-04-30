package com.gordeev.bankcards.mapper;

import com.gordeev.bankcards.dto.card.CardCreateRequest;
import com.gordeev.bankcards.dto.card.CardResponse;
import com.gordeev.bankcards.entity.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDate;

@Mapper(componentModel = "spring")
public interface CardMapper {
    // Из dto в entity
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "cardNumber", ignore = true)
    @Mapping(target = "cardHashNumber", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "expirationDate", expression = "java(generateExpirationDate())")
    @Mapping(target = "lastFourDigits", source = "cardNumber", qualifiedByName = "extractLastFourDigits")
    Card toCard(CardCreateRequest request);

    // Из entity в dto
    @Mapping(target = "maskedNumber", source = "lastFourDigits", qualifiedByName = "maskCardNumber")
    @Mapping(target = "userId", source = "user.id")
    CardResponse toResponse(Card card);


    // Вытягиваем последние 4 цифры карты
    @Named("extractLastFourDigits")
    default String extractLastFourDigits(String cardNumber) {
        if (cardNumber == null || cardNumber.length() != 16) {
            throw new IllegalArgumentException("Номер карты должен быть 16 символов");
        };
        return cardNumber.substring(12);
    }

    // Ставим маску номера карты
    @Named("maskCardNumber")
    default String maskCardNumber(String lastFourDigits) {
        return "*** *** *** " + lastFourDigits;
    }

    // Генерация срока карты (на 5 лет вперёд)
    @Named("generateExpirationDate")
    default LocalDate generateExpirationDate() {
        return LocalDate.now().plusYears(5);
    }
}
