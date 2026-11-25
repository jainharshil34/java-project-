package com.civicheat.dto.response;

import com.civicheat.enums.ComplaintStatus;
import com.civicheat.enums.IssueCategory;
import com.civicheat.enums.SeverityLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Detailed response representation of a civic complaint including media attachments and status history.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detailed complaint response with full audit trail and attachments")
public class ComplaintDetailResponse {

    private UUID id;
    private String title;
    private String description;
    private IssueCategory category;
    private Double latitude;
    private Double longitude;
    private String address;
    private Integer severityScore;
    private SeverityLevel severityLevel;
    private Map<String, Object> severityBreakdown;
    private ComplaintStatus status;
    private Integer upvoteCount;
    private UUID duplicateOfId;
    private Boolean duplicate;
    private UUID reporterId;
    private String reporterName;
    private String reporterEmail;
    private UUID departmentId;
    private String departmentName;

    @Builder.Default
    private List<ComplaintImageResponse> images = new ArrayList<>();

    @Builder.Default
    private List<ComplaintStatusHistoryResponse> statusHistories = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;
}
