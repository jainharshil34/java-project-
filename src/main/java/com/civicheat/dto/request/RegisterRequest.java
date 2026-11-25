package com.civicheat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Citizen registration request payload.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Citizen account registration request")
public class RegisterRequest {

    @NotBlank(message = "Email address is required")
    @Email(message = "Invalid email format")
    @Size(max = 120, message = "Email cannot exceed 120 characters")
    @Schema(description = "User email address", example = "jane.citizen@civicheat.local")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    @Schema(description = "Account password", example = "Secret123!")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
    @Schema(description = "User full name", example = "Jane Doe")
    private String fullName;

    @Size(max = 30, message = "Phone number cannot exceed 30 characters")
    @Schema(description = "Contact phone number", example = "+1-555-0199")
    private String phone;
}
