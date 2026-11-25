package com.civicheat.service;

import com.civicheat.dto.request.LoginRequest;
import com.civicheat.dto.request.RegisterRequest;
import com.civicheat.dto.response.AuthResponse;
import com.civicheat.dto.response.UserResponse;

/**
 * Service interface for citizen registration, login authentication, and user profile retrieval.
 */
public interface AuthService {

    /**
     * Registers a new citizen user account.
     *
     * @param request the registration details
     * @return authentication response containing token and user profile
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates a user with email and password credentials.
     *
     * @param request the login credentials
     * @return authentication response containing token and user profile
     */
    AuthResponse login(LoginRequest request);

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @param email the authenticated user's email
     * @return user profile response
     */
    UserResponse getCurrentUser(String email);
}
