package com.gordeev.bankcards.controller.user;

import com.gordeev.bankcards.dto.api.ApiError;
import com.gordeev.bankcards.dto.api.ApiResponse;
import com.gordeev.bankcards.dto.user.AuthRequest;
import com.gordeev.bankcards.dto.user.AuthResponse;
import com.gordeev.bankcards.dto.user.UserCreateRequest;
import com.gordeev.bankcards.dto.user.UserResponse;
import com.gordeev.bankcards.security.JwtService;
import com.gordeev.bankcards.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody @Valid AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

            String token = jwtService.generateToken(authentication.getName());

            return ResponseEntity.ok(ApiResponse.success(new AuthResponse(token)));


        } catch (BadCredentialsException | UsernameNotFoundException e) {
            ApiError error = new ApiError(
                    "Неправильное имя или пароль",
                    "BAD_AUTH",
                    null
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(error));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody @Valid UserCreateRequest request) {
        UserResponse result = userService.createUser(request);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
