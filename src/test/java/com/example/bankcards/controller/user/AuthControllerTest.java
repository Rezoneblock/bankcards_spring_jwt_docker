package com.gordeev.bankcards.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordeev.bankcards.dto.user.UserCreateRequest;
import com.gordeev.bankcards.dto.user.UserResponse;
import com.gordeev.bankcards.dto.user.auth.AuthRequest;
import com.gordeev.bankcards.dto.user.auth.AuthResponse;
import com.gordeev.bankcards.entity.Role;
import com.gordeev.bankcards.security.jwt.JwtService;
import com.gordeev.bankcards.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController MockMvc Тесты")
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID userId;
    private String username;
    private String password;
    private String token;
    private UserCreateRequest registerRequest;
    private UserResponse userResponse;
    private AuthRequest loginRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();

        userId = UUID.randomUUID();
        username = "testuser";
        password = "password123";
        token = "jwt-token-123456";

        registerRequest = new UserCreateRequest(
                username,
                "test@example.com",
                password
        );

        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        userResponse = UserResponse.builder()
                .id(userId)
                .username(username)
                .email("test@example.com")
                .enabled(true)
                .roles(Set.of(userRole))
                .build();

        loginRequest = new AuthRequest(username, password);
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("POST /api/v1/auth/login - успешный вход")
    void login_Success() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn(username);
        when(jwtService.generateToken(username)).thenReturn(token);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value(token));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - неверные учетные данные (BadCredentialsException)")
    void login_BadCredentials_ReturnsUnauthorized() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("Неправильное имя или пароль"))
                .andExpect(jsonPath("$.error.code").value("BAD_AUTH"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - пользователь не найден (UsernameNotFoundException)")
    void login_UserNotFound_ReturnsUnauthorized() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new UsernameNotFoundException("User not found"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("Неправильное имя или пароль"))
                .andExpect(jsonPath("$.error.code").value("BAD_AUTH"));
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("POST /api/v1/auth/register - успешная регистрация")
    void register_Success() throws Exception {
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - ошибка валидации (username слишком короткий)")
    void register_ValidationError_UsernameTooShort() throws Exception {
        UserCreateRequest invalidRequest = new UserCreateRequest("ab", "test@example.com", password);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - ошибка валидации (неверный email)")
    void register_ValidationError_InvalidEmail() throws Exception {
        UserCreateRequest invalidRequest = new UserCreateRequest(username, "invalid-email", password);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - ошибка валидации (пароль слишком короткий)")
    void register_ValidationError_PasswordTooShort() throws Exception {
        UserCreateRequest invalidRequest = new UserCreateRequest(username, "test@example.com", "12345");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - ошибка валидации (пустой username)")
    void register_ValidationError_EmptyUsername() throws Exception {
        UserCreateRequest invalidRequest = new UserCreateRequest("", "test@example.com", password);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - ошибка валидации (пустой email)")
    void register_ValidationError_EmptyEmail() throws Exception {
        UserCreateRequest invalidRequest = new UserCreateRequest(username, "", password);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - ошибка валидации (пустой password)")
    void register_ValidationError_EmptyPassword() throws Exception {
        UserCreateRequest invalidRequest = new UserCreateRequest(username, "test@example.com", "");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - ошибка валидации (null username)")
    void register_ValidationError_NullUsername() throws Exception {
        UserCreateRequest invalidRequest = new UserCreateRequest(null, "test@example.com", password);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - ошибка валидации (null email)")
    void register_ValidationError_NullEmail() throws Exception {
        UserCreateRequest invalidRequest = new UserCreateRequest(username, null, password);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - ошибка валидации (null password)")
    void register_ValidationError_NullPassword() throws Exception {
        UserCreateRequest invalidRequest = new UserCreateRequest(username, "test@example.com", null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}