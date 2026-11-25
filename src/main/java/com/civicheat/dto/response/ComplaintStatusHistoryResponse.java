package com.civicheat.dto.response;

import com.civicheat.enums.ComplaintStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Response representation of a complaint status transition audit record.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Complaint lifecycle status transition audit entry")
public class ComplaintStatusHistoryResponse {

    private UUID id;
    private ComplaintStatus previousStatus;
    private ComplaintStatus newStatus;
    private UUID changedByUserId;
    private String changedByUserName;
    private String remarks;
    private Instant createdAt;
}
