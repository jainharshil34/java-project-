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
import java.util.UUID;

/**
 * Standard summary response representation of a civic complaint.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Complaint summary response")
public class ComplaintResponse {

    private UUID id;
    private String title;
    private String description;
    private IssueCategory category;
    private Double latitude;
    private Double longitude;
    private String address;
    private Integer severityScore;
    private SeverityLevel severityLevel;
    private ComplaintStatus status;
    private Integer upvoteCount;
    private UUID duplicateOfId;
    private Boolean duplicate;
    private UUID reporterId;
    private String reporterName;
    private UUID departmentId;
    private String departmentName;

    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;
}
