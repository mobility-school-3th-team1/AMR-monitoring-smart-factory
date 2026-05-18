package com.sfaas.amr_control_system.config;

import com.sfaas.amr_control_system.entity.User;
import com.sfaas.amr_control_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DemoUserDataLoader implements CommandLineRunner {

    public static final String DEMO_USERNAME = "admin";
    public static final String DEMO_DISPLAY_NAME = "관리자";
    public static final String DEMO_ROLE = "admin";

    @Value("${app.demo.password:}")
    private String demoPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (demoPassword == null || demoPassword.isBlank()) {
            log.info("DEMO_USER_PASSWORD not set; skipping demo user seed.");
            return;
        }

        if (userRepository.findByUsername(DEMO_USERNAME).isPresent()) {
            return;
        }

        User demoUser = new User();
        demoUser.setUsername(DEMO_USERNAME);
        demoUser.setPassword(passwordEncoder.encode(demoPassword));
        demoUser.setDisplayName(DEMO_DISPLAY_NAME);
        demoUser.setRole(DEMO_ROLE);
        userRepository.save(demoUser);
    }
}
