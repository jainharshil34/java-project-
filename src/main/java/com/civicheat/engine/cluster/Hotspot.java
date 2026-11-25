package com.civicheat.engine.cluster;

import com.civicheat.enums.IssueCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Domain model representing an aggregated geographic hotspot cluster of civic incidents.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hotspot {

    private double centroidLat;
    private double centroidLng;
    private int count;
    private double averageSeverity;
    private IssueCategory dominantCategory;
    private double heatIntensity; // 0.0 - 1.0 normalized
    private double minLat;
    private double maxLat;
    private double minLon;
    private double maxLon;

    @Builder.Default
    private List<UUID> complaintIds = new ArrayList<>();
}
