package com.gordeev.bankcards.service;

import com.gordeev.bankcards.dto.user.UserCreateRequest;
import com.gordeev.bankcards.dto.user.UserResponse;
import com.gordeev.bankcards.entity.User;
import com.gordeev.bankcards.exception.ResourceAlreadyExistException;
import com.gordeev.bankcards.mapper.UserMapper;
import com.gordeev.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
    private static final String USER_NOT_FOUND = "Такого пользователя не существует";
    private static final String USER_ALREADY_EXISTS = "Пользователь уже существует";

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        User user = userMapper.toUser(request);

        if (userRepository.existsByUsername(user.getUsername()) || userRepository.existsByEmail(request.email())) {
            throw new ResourceAlreadyExistException(USER_ALREADY_EXISTS);
        }

        user.setPassword(passwordEncoder.encode(request.password()));

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }
}
