package com.shopease.service;

import com.shopease.dto.AuthDtos.*;
import com.shopease.entity.*;
import com.shopease.exception.BadRequestException;
import com.shopease.repository.UserRepository;
import com.shopease.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authentication;
    private final UserDetailsService userDetails;
    private final JwtService jwt;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw new BadRequestException("Email is already registered");
        User user = new User();
        user.setName(request.name().trim()); user.setEmail(email);
        user.setPassword(encoder.encode(request.password())); user.setRole(Role.USER);
        users.save(user);
        return response(user);
    }
    public AuthResponse login(LoginRequest request) {
        try {
            authentication.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException ex) {
            throw new BadRequestException("Invalid email or password");
        }
        User user = users.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        return response(user);
    }
    private AuthResponse response(User user) {
        String token = jwt.generate(userDetails.loadUserByUsername(user.getEmail()));
        return new AuthResponse(token, "Bearer", jwt.expirationSeconds(),
                new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name()));
    }
}
