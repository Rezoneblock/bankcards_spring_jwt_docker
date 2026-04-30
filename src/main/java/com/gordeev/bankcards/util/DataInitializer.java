package com.gordeev.bankcards.util;

import com.gordeev.bankcards.entity.Role;
import com.gordeev.bankcards.entity.User;
import com.gordeev.bankcards.repository.RoleRepository;
import com.gordeev.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

// ! Для удобства проверяющего создаю админа при запуске приложения !

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("Role ADMIN not found"));

            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Role USER not found"));

            Set<Role> roles = Set.of(adminRole, userRole);

            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .enabled(true)
                    .password(passwordEncoder.encode(adminPassword))
                    .roles(roles)
                    .build();

            userRepository.save(admin);

            System.out.println("=============================================");
            System.out.println("Администратор успешно создан: " + adminUsername);
            System.out.println("=============================================");
        } else {
            System.out.println("=============================================");
            System.out.println("Администратор: " + adminUsername);
            System.out.println("=============================================");
        }
    }
}
