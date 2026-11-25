package com.civicheat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Request payload to reassign a complaint to a different municipal department.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload to reassign complaint department")
public class ReassignComplaintRequest {

    @NotNull(message = "Target department ID is required")
    @Schema(description = "UUID of the department to reassign the complaint to", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID departmentId;

    @Size(max = 500, message = "Reassignment reason cannot exceed 500 characters")
    @Schema(description = "Reason for departmental reassignment", example = "Issue involves electrical wiring beneath road surface")
    private String reason;
}
