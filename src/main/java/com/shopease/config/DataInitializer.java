package com.shopease.config;

import com.shopease.entity.*;
import com.shopease.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean CommandLineRunner createAdmin(UserRepository users, PasswordEncoder encoder,
            @Value("${app.admin.email}") String email, @Value("${app.admin.password}") String password) {
        return args -> {
            if (!users.existsByEmailIgnoreCase(email)) {
                User admin = new User(); admin.setName("ShopEase Admin"); admin.setEmail(email.toLowerCase());
                admin.setPassword(encoder.encode(password)); admin.setRole(Role.ADMIN); users.save(admin);
            }
        };
    }
}
