package com.civicheat.engine.cluster;

import com.civicheat.entity.Complaint;
import com.civicheat.enums.IssueCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Grid-based spatial clustering algorithm for aggregating civic complaints into hotspot zones.
 */
@Slf4j
@Component
public class GridHotspotClusterer implements HotspotClusterer {

    public static final double METERS_PER_DEGREE_LAT = 111320.0;
    public static final double DEFAULT_CELL_SIZE_METERS = 200.0;
    public static final int DEFAULT_MIN_HOTSPOT_COUNT = 2;

    @Override
    public List<Hotspot> cluster(List<Complaint> complaints, double cellSizeMeters) {
        if (complaints == null || complaints.isEmpty()) {
            return Collections.emptyList();
        }

        double effectiveCellSize = cellSizeMeters > 0 ? cellSizeMeters : DEFAULT_CELL_SIZE_METERS;
        double deltaLat = effectiveCellSize / METERS_PER_DEGREE_LAT;

        // Group complaints by spatial grid cell key
        Map<CellKey, List<Complaint>> cellMap = complaints.stream()
                .filter(c -> c != null && c.getLatitude() != null && c.getLongitude() != null)
                .collect(Collectors.groupingBy(c -> computeCellKey(c.getLatitude(), c.getLongitude(), deltaLat)));

        if (cellMap.isEmpty()) {
            return Collections.emptyList();
        }

        int minCount = complaints.size() <= 2 ? 1 : DEFAULT_MIN_HOTSPOT_COUNT;

        List<RawCell> rawCells = new ArrayList<>();
        double maxRawIntensity = 0.0;

        for (Map.Entry<CellKey, List<Complaint>> entry : cellMap.entrySet()) {
            List<Complaint> cellComplaints = entry.getValue();
            if (cellComplaints.size() < minCount) {
                continue;
            }

            CellKey key = entry.getKey();
            int count = cellComplaints.size();

            double avgLat = cellComplaints.stream()
                    .mapToDouble(Complaint::getLatitude)
                    .average()
                    .orElse(0.0);

            double avgLng = cellComplaints.stream()
                    .mapToDouble(Complaint::getLongitude)
                    .average()
                    .orElse(0.0);

            double avgSeverity = cellComplaints.stream()
                    .mapToDouble(c -> c.getSeverityScore() != null ? c.getSeverityScore() : 0.0)
                    .average()
                    .orElse(0.0);

            IssueCategory dominantCategory = cellComplaints.stream()
                    .map(Complaint::getCategory)
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(IssueCategory.POTHOLE);

            List<UUID> complaintIds = cellComplaints.stream()
                    .map(Complaint::getId)
                    .filter(Objects::nonNull)
                    .toList();

            double cellLat = key.latIndex * deltaLat;
            double deltaLng = deltaLat / Math.max(0.01, Math.cos(Math.toRadians(cellLat)));
            double minLat = key.latIndex * deltaLat;
            double maxLat = (key.latIndex + 1) * deltaLat;
            double minLon = key.lngIndex * deltaLng;
            double maxLon = (key.lngIndex + 1) * deltaLng;

            // Raw intensity scales with both volume and severity
            double rawIntensity = count * (0.5 + 0.5 * (avgSeverity / 100.0));
            if (rawIntensity > maxRawIntensity) {
                maxRawIntensity = rawIntensity;
            }

            rawCells.add(new RawCell(
                    avgLat, avgLng, count, avgSeverity, dominantCategory,
                    minLat, maxLat, minLon, maxLon, complaintIds, rawIntensity
            ));
        }

        if (rawCells.isEmpty()) {
            return Collections.emptyList();
        }

        final double finalMaxIntensity = maxRawIntensity > 0 ? maxRawIntensity : 1.0;

        return rawCells.stream()
                .map(rc -> {
                    double normalizedHeat = Math.min(1.0, Math.max(0.0, rc.rawIntensity / finalMaxIntensity));
                    double roundedHeat = Math.round(normalizedHeat * 1000.0) / 1000.0;

                    return Hotspot.builder()
                            .centroidLat(Math.round(rc.centroidLat * 1000000.0) / 1000000.0)
                            .centroidLng(Math.round(rc.centroidLng * 1000000.0) / 1000000.0)
                            .count(rc.count)
                            .averageSeverity(Math.round(rc.avgSeverity * 10.0) / 10.0)
                            .dominantCategory(rc.dominantCategory)
                            .heatIntensity(roundedHeat)
                            .minLat(rc.minLat)
                            .maxLat(rc.maxLat)
                            .minLon(rc.minLon)
                            .maxLon(rc.maxLon)
                            .complaintIds(rc.complaintIds)
                            .build();
                })
                .sorted(Comparator.comparingDouble(Hotspot::getHeatIntensity)
                        .thenComparingInt(Hotspot::getCount)
                        .reversed())
                .toList();
    }

    private CellKey computeCellKey(double lat, double lon, double deltaLat) {
        long latIdx = (long) Math.floor(lat / deltaLat);
        double cellLat = latIdx * deltaLat;
        double deltaLng = deltaLat / Math.max(0.01, Math.cos(Math.toRadians(cellLat)));
        long lngIdx = (long) Math.floor(lon / deltaLng);
        return new CellKey(latIdx, lngIdx);
    }

    public record CellKey(long latIndex, long lngIndex) {}

    private record RawCell(
            double centroidLat,
            double centroidLng,
            int count,
            double avgSeverity,
            IssueCategory dominantCategory,
            double minLat,
            double maxLat,
            double minLon,
            double maxLon,
            List<UUID> complaintIds,
            double rawIntensity
    ) {}
}
