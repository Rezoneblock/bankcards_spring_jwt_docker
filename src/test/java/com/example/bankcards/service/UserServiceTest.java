package com.example.bankcards.service;

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
import com.gordeev.bankcards.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService тесты")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private UUID adminId;
    private User user;
    private UserCreateRequest createRequest;
    private UserResponse userResponse;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ADMIN");

        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setEnabled(true);
        user.setRoles(Set.of(userRole));

        createRequest = new UserCreateRequest("testuser", "test@example.com", "password123");

        userResponse = UserResponse.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .enabled(true)
                .roles(Set.of(userRole))
                .build();
    }

    // Create user

    @Test
    @DisplayName("Успешная регистрация пользователя")
    void createUser_Success() {
        when(userMapper.toUser(createRequest)).thenReturn(user);
        when(userRepository.existsByUsername(user.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(createRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(createRequest.password())).thenReturn("encodedPassword");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.createUser(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.enabled()).isTrue();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Ошибка при регистрации - username уже существует")
    void createUser_DuplicateUsername_ThrowsException() {
        when(userMapper.toUser(createRequest)).thenReturn(user);
        when(userRepository.existsByUsername(user.getUsername())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(ResourceAlreadyExistException.class)
                .hasMessageContaining(UserService.USER_ALREADY_EXISTS);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Ошибка при регистрации - email уже существует")
    void createUser_DuplicateEmail_ThrowsException() {
        when(userMapper.toUser(createRequest)).thenReturn(user);
        when(userRepository.existsByUsername(user.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(createRequest.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(ResourceAlreadyExistException.class)
                .hasMessageContaining(UserService.USER_ALREADY_EXISTS);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Ошибка при регистрации - роль USER не найдена")
    void createUser_RoleNotFound_ThrowsException() {
        when(userMapper.toUser(createRequest)).thenReturn(user);
        when(userRepository.existsByUsername(user.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(createRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(createRequest.password())).thenReturn("encodedPassword");
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Role USER not found");

        verify(userRepository, never()).save(any(User.class));
    }

    // Update user status

    @Test
    @DisplayName("Админ блокирует пользователя")
    void updateUserStatus_AdminBlocksUser_Success() {
        UserStatusUpdateRequest request = new UserStatusUpdateRequest(userId, false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.updateUserStatus(adminId, request);

        assertThat(result).isNotNull();
        assertThat(user.isEnabled()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Админ разблокирует пользователя")
    void updateUserStatus_AdminUnblocksUser_Success() {
        user.setEnabled(false);
        UserStatusUpdateRequest request = new UserStatusUpdateRequest(userId, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.updateUserStatus(adminId, request);

        assertThat(result).isNotNull();
        assertThat(user.isEnabled()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Ошибка - пользователь не найден")
    void updateUserStatus_UserNotFound_ThrowsException() {
        UserStatusUpdateRequest request = new UserStatusUpdateRequest(userId, false);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserStatus(adminId, request))
                .isInstanceOf(ResourceDoesNotExistException.class)
                .hasMessageContaining(UserService.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Ошибка - нельзя заблокировать администратора")
    void updateUserStatus_CannotBlockAdmin_ThrowsException() {
        user.setRoles(Set.of(adminRole));
        UserStatusUpdateRequest request = new UserStatusUpdateRequest(userId, false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateUserStatus(adminId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CANNOT_CHANGE_ADMIN_STATUS");
    }

    @Test
    @DisplayName("Ошибка - нельзя заблокировать самого себя")
    void updateUserStatus_CannotBlockSelf_ThrowsException() {
        UserStatusUpdateRequest request = new UserStatusUpdateRequest(userId, false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateUserStatus(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Нельзя изменить статус самого себя");
    }

    // Get all users

    @Test
    @DisplayName("Успешное получение всех пользователей с пагинацией")
    void getAllUsers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        PageResponse<UserResponse> result = userService.getAllUsers(pageable);

        assertThat(result).isNotNull();
        assertThat(result.entities()).hasSize(1);
        assertThat(result.meta().totalElements()).isEqualTo(1);
        assertThat(result.meta().currentPageNumber()).isEqualTo(0);
        assertThat(result.meta().size()).isEqualTo(10);

        verify(userRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Получение пустой страницы пользователей")
    void getAllUsers_EmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(userRepository.findAll(pageable)).thenReturn(emptyPage);

        PageResponse<UserResponse> result = userService.getAllUsers(pageable);

        assertThat(result).isNotNull();
        assertThat(result.entities()).isEmpty();
        assertThat(result.meta().totalElements()).isEqualTo(0);

        verify(userRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Получение пользователей - проверка метаданных пагинации")
    void getAllUsers_CheckMetadata() {
        Pageable pageable = PageRequest.of(2, 5);
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 25);

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        PageResponse<UserResponse> result = userService.getAllUsers(pageable);

        assertThat(result).isNotNull();
        assertThat(result.meta().currentPageNumber()).isEqualTo(2);
        assertThat(result.meta().size()).isEqualTo(5);
        assertThat(result.meta().totalElements()).isEqualTo(25);
        assertThat(result.meta().totalPages()).isEqualTo(5);
    }
}