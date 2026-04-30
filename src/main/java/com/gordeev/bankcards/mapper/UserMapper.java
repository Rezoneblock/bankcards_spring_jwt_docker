package com.gordeev.bankcards.mapper;

import com.gordeev.bankcards.dto.user.UserCreateRequest;
import com.gordeev.bankcards.dto.user.UserResponse;
import com.gordeev.bankcards.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    // Из dto в entity
    @Mapping(target = "password", ignore = true)
    User toUser(UserCreateRequest request);

    // Из entity в dto
    UserResponse toResponse(User user);
}
