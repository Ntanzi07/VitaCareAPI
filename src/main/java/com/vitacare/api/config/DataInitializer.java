package com.vitacare.api.config;

import com.vitacare.api.model.User;
import com.vitacare.api.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        // It will not stay here for the long term, but for development purposes,
        // we need to have at least one admin user to manage the system.
        return args -> {
            if (userRepository.findByEmail("admin@vitacare.com").isEmpty()) {
                User admin = new User();
                admin.setEmail("admin@vitacare.com");
                admin.setPasswordHash(passwordEncoder.encode("admin123"));
                admin.setRole("ADMIN");
                userRepository.save(admin);
            }
        };
    }
}