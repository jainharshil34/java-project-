package com.civicheat.service;

import com.civicheat.dto.response.HeatmapStatsResponse;
import com.civicheat.dto.response.HotspotResponse;
import com.civicheat.enums.IssueCategory;

import java.util.List;

/**
 * Service interface governing heatmap data layer generation, spatial hotspot clustering, and civic analytics.
 */
public interface HeatmapService {

    /**
     * Generates lightweight geospatial weighted coordinate triples [latitude, longitude, intensity]
     * formatted for consumption by mapping libraries such as Leaflet.heat or Mapbox GL.
     */
    List<double[]> getHeatmapPoints(
            Double minLat, Double maxLat, Double minLon, Double maxLon,
            IssueCategory category, Integer days
    );

    /**
     * Identifies and ranks dense geographical hotspot clusters of civic problems.
     */
    List<HotspotResponse> getHotspots(
            Double minLat, Double maxLat, Double minLon, Double maxLon,
            IssueCategory category, Integer days, Double cellSizeMeters
    );

    /**
     * Compiles platform-wide statistics including category totals, status distribution, and resolution metrics.
     */
    HeatmapStatsResponse getPlatformStats();
}
