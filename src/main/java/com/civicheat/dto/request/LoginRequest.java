package com.civicheat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Authentication login request payload.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User login credentials")
public class LoginRequest {

    @NotBlank(message = "Email address is required")
    @Email(message = "Invalid email format")
    @Schema(description = "User email address", example = "jane.citizen@civicheat.local")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "Account password", example = "Secret123!")
    private String password;
}
