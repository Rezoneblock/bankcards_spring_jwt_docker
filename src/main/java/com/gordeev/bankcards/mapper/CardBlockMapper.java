package com.gordeev.bankcards.mapper;

import com.gordeev.bankcards.dto.card.block.CardBlockResponse;
import com.gordeev.bankcards.entity.CardBlock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CardBlockMapper {
    @Mapping(target = "cardId", source = "card.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "maskedNumber", source = "card.lastFourDigits", qualifiedByName = "maskCardNumber")
    CardBlockResponse toResponse(CardBlock entity);

    @Named("maskCardNumber")
    default String maskCardNumber(String lastFourDigits) {
        return "**** **** **** " + lastFourDigits;
    }
}
