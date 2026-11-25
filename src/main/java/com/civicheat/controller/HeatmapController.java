package com.civicheat.controller;

import com.civicheat.dto.response.HeatmapStatsResponse;
import com.civicheat.dto.response.HotspotResponse;
import com.civicheat.enums.IssueCategory;
import com.civicheat.service.HeatmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller exposing public geospatial heatmap tiles, hotspot clusters, and platform analytics.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/heatmap")
@RequiredArgsConstructor
@Tag(name = "Heatmap & Hotspots", description = "Endpoints for Leaflet.heat weighted tiles, spatial cluster analytics, and civic stats")
public class HeatmapController {

    private final HeatmapService heatmapService;

    @GetMapping("/points")
    @Operation(
            summary = "Get weighted points for heatmap rendering",
            description = "Returns lightweight array of [latitude, longitude, intensity] triples suitable for Leaflet.heat or Mapbox GL"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of weighted coordinate triples")
    })
    public ResponseEntity<List<double[]>> getHeatmapPoints(
            @Parameter(description = "Spatial bounding box formatted as minLat,minLon,maxLat,maxLon", example = "37.70,-122.50,37.85,-122.35")
            @RequestParam(required = false) String bbox,

            @Parameter(description = "Filter by issue category")
            @RequestParam(required = false) IssueCategory category,

            @Parameter(description = "Filter incidents submitted within the past N days", example = "30")
            @RequestParam(required = false) Integer days) {

        ParsedBBox parsed = parseBbox(bbox);
        List<double[]> points = heatmapService.getHeatmapPoints(
                parsed.minLat, parsed.maxLat, parsed.minLon, parsed.maxLon,
                category, days
        );
        return ResponseEntity.ok(points);
    }

    @GetMapping("/hotspots")
    @Operation(
            summary = "Get aggregated hotspot clusters",
            description = "Returns ranked hotspot clusters computed by spatial grid aggregation"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranked list of geographical hotspot clusters")
    })
    public ResponseEntity<List<HotspotResponse>> getHotspots(
            @Parameter(description = "Spatial bounding box formatted as minLat,minLon,maxLat,maxLon", example = "37.70,-122.50,37.85,-122.35")
            @RequestParam(required = false) String bbox,

            @Parameter(description = "Filter by issue category")
            @RequestParam(required = false) IssueCategory category,

            @Parameter(description = "Filter incidents submitted within the past N days", example = "30")
            @RequestParam(required = false) Integer days,

            @Parameter(description = "Grid cell size in meters", example = "200.0")
            @RequestParam(required = false, defaultValue = "200.0") Double cellSize) {

        ParsedBBox parsed = parseBbox(bbox);
        List<HotspotResponse> hotspots = heatmapService.getHotspots(
                parsed.minLat, parsed.maxLat, parsed.minLon, parsed.maxLon,
                category, days, cellSize
        );
        return ResponseEntity.ok(hotspots);
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Get platform-wide civic stats and metrics",
            description = "Returns aggregate totals by category, status, department, average resolution times, and severity scores"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Platform statistics successfully retrieved")
    })
    public ResponseEntity<HeatmapStatsResponse> getPlatformStats() {
        return ResponseEntity.ok(heatmapService.getPlatformStats());
    }

    private ParsedBBox parseBbox(String bbox) {
        if (bbox == null || bbox.isBlank()) {
            return new ParsedBBox(null, null, null, null);
        }
        try {
            String[] parts = bbox.split(",");
            if (parts.length == 4) {
                double minLat = Double.parseDouble(parts[0].trim());
                double minLon = Double.parseDouble(parts[1].trim());
                double maxLat = Double.parseDouble(parts[2].trim());
                double maxLon = Double.parseDouble(parts[3].trim());
                return new ParsedBBox(minLat, maxLat, minLon, maxLon);
            }
        } catch (NumberFormatException ex) {
            log.warn("Invalid bbox parameter format: {}", bbox);
        }
        return new ParsedBBox(null, null, null, null);
    }

    private record ParsedBBox(Double minLat, Double maxLat, Double minLon, Double maxLon) {}
}
