package com.civicheat.dto.response;

import com.civicheat.enums.IssueCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * API response representation of an aggregated hotspot cluster.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Aggregated geographical hotspot cluster")
public class HotspotResponse {

    @Schema(description = "Centroid latitude of the hotspot", example = "37.7749")
    private double centroidLat;

    @Schema(description = "Centroid longitude of the hotspot", example = "-122.4194")
    private double centroidLng;

    @Schema(description = "Total number of incidents in this cluster", example = "8")
    private int count;

    @Schema(description = "Average severity score of incidents in the cluster", example = "62.5")
    private double averageSeverity;

    @Schema(description = "Most frequent issue category in this cluster", example = "POTHOLE")
    private IssueCategory dominantCategory;

    @Schema(description = "Normalized heat intensity value (0.0 to 1.0)", example = "0.85")
    private double heatIntensity;

    private double minLat;
    private double maxLat;
    private double minLon;
    private double maxLon;

    @Builder.Default
    private List<UUID> complaintIds = new ArrayList<>();
}
