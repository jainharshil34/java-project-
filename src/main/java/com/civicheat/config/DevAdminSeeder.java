package com.civicheat.config;

import com.civicheat.entity.User;
import com.civicheat.enums.Role;
import com.civicheat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds default administrator credentials when running under the dev profile.
 */
@Slf4j
@Component
@Profile({"dev", "local"})
@RequiredArgsConstructor
public class DevAdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public static final String DEFAULT_ADMIN_EMAIL = "admin@civicheat.gov";
    public static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail(DEFAULT_ADMIN_EMAIL)) {
            User admin = User.builder()
                    .email(DEFAULT_ADMIN_EMAIL)
                    .password(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
                    .fullName("System Administrator")
                    .phone("+1-555-0100")
                    .role(Role.ADMIN)
                    .build();

            userRepository.save(admin);
            log.info("Initialized default dev administrator account: email={}", DEFAULT_ADMIN_EMAIL);
        } else {
            log.info("Dev administrator account already present: email={}", DEFAULT_ADMIN_EMAIL);
        }
    }
}
