package com.civicheat.service.impl;

import com.civicheat.dto.request.LoginRequest;
import com.civicheat.dto.request.RegisterRequest;
import com.civicheat.dto.response.AuthResponse;
import com.civicheat.dto.response.UserResponse;
import com.civicheat.entity.User;
import com.civicheat.enums.Role;
import com.civicheat.exception.DuplicateResourceException;
import com.civicheat.exception.ResourceNotFoundException;
import com.civicheat.mapper.UserMapper;
import com.civicheat.repository.UserRepository;
import com.civicheat.security.JwtTokenProvider;
import com.civicheat.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of AuthService handling citizen self-registration, credential verification, and JWT generation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Processing citizen registration for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new DuplicateResourceException("An account with email " + request.getEmail() + " already exists");
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .role(Role.CITIZEN)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(savedUser);

        log.info("Successfully registered citizen account: id={}, email={}", savedUser.getId(), savedUser.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInSeconds(jwtTokenProvider.getExpirationInSeconds())
                .user(userMapper.toResponse(savedUser))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("Processing login attempt for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt for email: {}", email);
            throw new BadCredentialsException("Invalid email or password credentials");
        }

        String token = jwtTokenProvider.generateToken(user);
        log.info("User {} logged in successfully with role {}", user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInSeconds(jwtTokenProvider.getExpirationInSeconds())
                .user(userMapper.toResponse(user))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for email: " + email));
        return userMapper.toResponse(user);
    }
}
