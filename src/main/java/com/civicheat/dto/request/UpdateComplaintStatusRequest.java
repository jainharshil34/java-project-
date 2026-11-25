package com.civicheat.dto.request;

import com.civicheat.enums.ComplaintStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for updating complaint lifecycle status.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload to update complaint status")
public class UpdateComplaintStatusRequest {

    @NotNull(message = "New complaint status is required")
    @Schema(description = "Target complaint status", example = "IN_PROGRESS")
    private ComplaintStatus status;

    @Size(max = 500, message = "Status remark cannot exceed 500 characters")
    @Schema(description = "Optional remarks explaining the transition", example = "Assigned field team to inspect damage")
    private String remark;
}
