package com.civicheat.dto.response;

import com.civicheat.enums.ComplaintStatus;
import com.civicheat.enums.IssueCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Summary analytics metrics and status distributions across the platform.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Overall civic platform analytics and resolution metrics")
public class HeatmapStatsResponse {

    @Schema(description = "Total number of complaints in system", example = "142")
    private long totalComplaints;

    @Schema(description = "Total number of open/active complaints", example = "58")
    private long activeComplaints;

    @Schema(description = "Total number of resolved complaints", example = "84")
    private long resolvedComplaints;

    @Schema(description = "Average resolution time in hours for resolved complaints", example = "36.4")
    private Double averageResolutionTimeHours;

    @Schema(description = "Average severity score across all active complaints", example = "48.2")
    private Double averageSeverityScore;

    @Builder.Default
    @Schema(description = "Breakdown of complaints by category")
    private Map<IssueCategory, Long> categoryCounts = new HashMap<>();

    @Builder.Default
    @Schema(description = "Breakdown of complaints by lifecycle status")
    private Map<ComplaintStatus, Long> statusCounts = new HashMap<>();

    @Builder.Default
    @Schema(description = "Breakdown of complaints by municipal department")
    private Map<String, Long> departmentCounts = new HashMap<>();
}
