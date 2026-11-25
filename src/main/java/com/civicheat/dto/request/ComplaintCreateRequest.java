package com.civicheat.dto.request;

import com.civicheat.enums.IssueCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for creating a new civic complaint.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for submitting a new geotagged civic complaint")
public class ComplaintCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 120, message = "Title must be between 5 and 120 characters")
    @Schema(description = "Brief title of the civic issue", example = "Deep pothole causing traffic slowdown")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    @Schema(description = "Detailed description of the issue", example = "A large 2-foot wide pothole right at the intersection of 5th Ave and Main St.")
    private String description;

    @NotNull(message = "Category is required")
    @Schema(description = "Category of civic issue", example = "POTHOLE")
    private IssueCategory category;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90.0 and 90.0")
    @Schema(description = "Geographic latitude coordinate", example = "37.774929")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180.0 and 180.0")
    @Schema(description = "Geographic longitude coordinate", example = "-122.419416")
    private Double longitude;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    @Schema(description = "Optional physical landmark or street address", example = "5th Ave & Main St, Downtown")
    private String address;
}
