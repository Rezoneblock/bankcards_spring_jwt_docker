package com.gordeev.bankcards.mapper;

import com.gordeev.bankcards.dto.card.CardCreateRequest;
import com.gordeev.bankcards.dto.card.CardResponse;
import com.gordeev.bankcards.entity.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CardMapper {
    // Из dto в entity
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "lastFourDigits", source = "cardNumber", qualifiedByName = "extractLastFourDigits")
    Card toCard(CardCreateRequest request);

    // Из entity в dto
    @Mapping(target = "maskedNumber", source = "lastFourDigits", qualifiedByName = "maskCardNumber")
    CardResponse toResponse(Card card);


    // Вытягиваем последние 4 цифры карты
    @Named("extractLastFourDigits")
    default String extractLastFourDigits(String cardNumber) {
        if (cardNumber == null || cardNumber.length() != 16) {
            throw new IllegalArgumentException("Номер карты должен быть 16 символов");
        };
        return cardNumber.substring(12);
    }

    // Ставим маску
    @Named("maskCardNumber")
    default String maskCardNumber(String lastFourDigits) {
        return "*** *** *** " + lastFourDigits;
    }
}
