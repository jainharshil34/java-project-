package com.civicheat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Response representation of a municipal department.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Municipal department details and status counts")
public class DepartmentResponse {

    private UUID id;
    private String name;
    private String code;
    private String description;
    private String contactEmail;
    private String contactPhone;
    private Long openComplaintsCount;
    private Long resolvedComplaintsCount;
    private Instant createdAt;
}
