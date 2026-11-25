package com.civicheat.dto.response;

import com.civicheat.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * User account response profile.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User profile information")
public class UserResponse {

    @Schema(description = "Unique user identifier", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
    private UUID id;

    @Schema(description = "User email address", example = "citizen@civicheat.local")
    private String email;

    @Schema(description = "User full name", example = "Jane Doe")
    private String fullName;

    @Schema(description = "Contact phone number", example = "+1-555-0199")
    private String phone;

    @Schema(description = "Assigned platform role", example = "CITIZEN")
    private Role role;

    @Schema(description = "Assigned department ID (if Officer)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID departmentId;

    @Schema(description = "Assigned department name (if Officer)", example = "Sanitation")
    private String departmentName;

    @Schema(description = "Account registration timestamp")
    private Instant createdAt;
}
