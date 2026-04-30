package com.gordeev.bankcards.service;

import com.gordeev.bankcards.dto.api.PageResponse;
import com.gordeev.bankcards.dto.user.UserCreateRequest;
import com.gordeev.bankcards.dto.user.UserResponse;
import com.gordeev.bankcards.dto.user.UserStatusUpdateRequest;
import com.gordeev.bankcards.entity.Role;
import com.gordeev.bankcards.entity.User;
import com.gordeev.bankcards.exception.ResourceAlreadyExistException;
import com.gordeev.bankcards.exception.ResourceDoesNotExistException;
import com.gordeev.bankcards.mapper.UserMapper;
import com.gordeev.bankcards.repository.RoleRepository;
import com.gordeev.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    public static final String USER_NOT_FOUND = "Пользователя не существует";
    public static final String USER_ALREADY_EXISTS = "Пользователь уже существует";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    public UserResponse createUser(UserCreateRequest request) {
        User user = userMapper.toUser(request);

        if (userRepository.existsByUsername(user.getUsername()) || userRepository.existsByEmail(request.email())) {
            throw new ResourceAlreadyExistException(USER_ALREADY_EXISTS);
        }

        user.setPassword(passwordEncoder.encode(request.password()));

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Role USER not found"));

        user.setRoles(Set.of(userRole));

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    public UserResponse updateUserStatus(UUID userId, UserStatusUpdateRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceDoesNotExistException(USER_NOT_FOUND));

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));
        if (isAdmin) {
            throw new IllegalStateException("CANNOT_CHANGE_ADMIN_STATUS");
        }

        if (user.getId().equals(userId)) {
            throw new IllegalStateException("Нельзя изменить статус самого себя");
        }

        user.setEnabled(request.enabled());
        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);

        return new PageResponse<>(
                page.map(userMapper::toResponse).getContent(),
                new PageResponse.Metadata(
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.getNumber()
                )
        );
    }
}
