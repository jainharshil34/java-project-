package com.civicheat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Authentication response containing JWT token and user profile summary.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Authentication token response")
public class AuthResponse {

    @Schema(description = "Bearer JWT access token")
    private String token;

    @Schema(description = "Token type", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Token validity in seconds", example = "86400")
    private Long expiresInSeconds;

    @Schema(description = "Authenticated user profile")
    private UserResponse user;
}
