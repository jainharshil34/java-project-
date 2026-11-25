package com.civicheat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Admin request payload to register a departmental officer.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Department officer registration request")
public class CreateOfficerRequest {

    @NotBlank(message = "Email address is required")
    @Email(message = "Invalid email format")
    @Size(max = 120, message = "Email cannot exceed 120 characters")
    @Schema(description = "Officer email address", example = "officer.smith@civicheat.gov")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    @Schema(description = "Initial officer password", example = "OfficerPass123!")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
    @Schema(description = "Officer full name", example = "Officer John Smith")
    private String fullName;

    @Size(max = 30, message = "Phone number cannot exceed 30 characters")
    @Schema(description = "Officer contact phone", example = "+1-555-0188")
    private String phone;

    @NotNull(message = "Department ID is required")
    @Schema(description = "Assigned department UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID departmentId;
}
