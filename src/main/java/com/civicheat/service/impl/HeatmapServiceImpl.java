package com.civicheat.service.impl;

import com.civicheat.config.CacheConfig;
import com.civicheat.dto.response.HeatmapStatsResponse;
import com.civicheat.dto.response.HotspotResponse;
import com.civicheat.engine.cluster.Hotspot;
import com.civicheat.engine.cluster.HotspotClusterer;
import com.civicheat.entity.Complaint;
import com.civicheat.entity.Department;
import com.civicheat.enums.ComplaintStatus;
import com.civicheat.enums.IssueCategory;
import com.civicheat.repository.ComplaintRepository;
import com.civicheat.repository.ComplaintSpecification;
import com.civicheat.repository.DepartmentRepository;
import com.civicheat.service.HeatmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Production implementation of HeatmapService with clustering integration and cached responses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeatmapServiceImpl implements HeatmapService {

    private final ComplaintRepository complaintRepository;
    private final DepartmentRepository departmentRepository;
    private final HotspotClusterer hotspotClusterer;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_HEATMAP_POINTS, key = "{#minLat, #maxLat, #minLon, #maxLon, #category, #days}")
    public List<double[]> getHeatmapPoints(
            Double minLat, Double maxLat, Double minLon, Double maxLon,
            IssueCategory category, Integer days) {

        log.debug("Generating heatmap points: bbox=[{},{},{},{}], category={}, days={}",
                minLat, maxLat, minLon, maxLon, category, days);

        List<Complaint> complaints = queryComplaints(minLat, maxLat, minLon, maxLon, category, days);

        List<double[]> points = new ArrayList<>(complaints.size());
        for (Complaint c : complaints) {
            if (c.getLatitude() != null && c.getLongitude() != null) {
                double score = c.getSeverityScore() != null ? c.getSeverityScore() : 25.0;
                double weight = Math.max(0.1, Math.min(1.0, score / 100.0));
                points.add(new double[]{c.getLatitude(), c.getLongitude(), Math.round(weight * 100.0) / 100.0});
            }
        }

        return points;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_HEATMAP_HOTSPOTS, key = "{#minLat, #maxLat, #minLon, #maxLon, #category, #days, #cellSizeMeters}")
    public List<HotspotResponse> getHotspots(
            Double minLat, Double maxLat, Double minLon, Double maxLon,
            IssueCategory category, Integer days, Double cellSizeMeters) {

        log.debug("Clustering hotspots: bbox=[{},{},{},{}], category={}, days={}, cellSize={}",
                minLat, maxLat, minLon, maxLon, category, days, cellSizeMeters);

        List<Complaint> complaints = queryComplaints(minLat, maxLat, minLon, maxLon, category, days);
        double cellSize = (cellSizeMeters != null && cellSizeMeters > 0) ? cellSizeMeters : 200.0;

        List<Hotspot> hotspots = hotspotClusterer.cluster(complaints, cellSize);

        return hotspots.stream().map(h -> HotspotResponse.builder()
                .centroidLat(h.getCentroidLat())
                .centroidLng(h.getCentroidLng())
                .count(h.getCount())
                .averageSeverity(h.getAverageSeverity())
                .dominantCategory(h.getDominantCategory())
                .heatIntensity(h.getHeatIntensity())
                .minLat(h.getMinLat())
                .maxLat(h.getMaxLat())
                .minLon(h.getMinLon())
                .maxLon(h.getMaxLon())
                .complaintIds(h.getComplaintIds())
                .build()
        ).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_HEATMAP_STATS)
    public HeatmapStatsResponse getPlatformStats() {
        log.debug("Compiling platform stats...");

        List<Complaint> allComplaints = complaintRepository.findAll();
        long total = allComplaints.size();

        Map<IssueCategory, Long> categoryCounts = new HashMap<>();
        for (IssueCategory cat : IssueCategory.values()) {
            categoryCounts.put(cat, 0L);
        }

        Map<ComplaintStatus, Long> statusCounts = new HashMap<>();
        for (ComplaintStatus st : ComplaintStatus.values()) {
            statusCounts.put(st, 0L);
        }

        long resolvedCount = 0;
        long totalResolutionHours = 0;
        long resolvedWithTimeCount = 0;
        double totalSeverity = 0;
        int complaintsWithSeverity = 0;

        for (Complaint c : allComplaints) {
            if (c.getCategory() != null) {
                categoryCounts.put(c.getCategory(), categoryCounts.getOrDefault(c.getCategory(), 0L) + 1);
            }
            if (c.getStatus() != null) {
                statusCounts.put(c.getStatus(), statusCounts.getOrDefault(c.getStatus(), 0L) + 1);
            }

            if (c.getStatus() == ComplaintStatus.RESOLVED) {
                resolvedCount++;
                if (c.getResolvedAt() != null && c.getCreatedAt() != null) {
                    long hours = Duration.between(c.getCreatedAt(), c.getResolvedAt()).toHours();
                    totalResolutionHours += Math.max(0, hours);
                    resolvedWithTimeCount++;
                }
            }

            if (c.getSeverityScore() != null) {
                totalSeverity += c.getSeverityScore();
                complaintsWithSeverity++;
            }
        }

        long activeCount = total - resolvedCount - statusCounts.getOrDefault(ComplaintStatus.REJECTED, 0L)
                - statusCounts.getOrDefault(ComplaintStatus.DUPLICATE, 0L);

        Double avgResolutionTime = resolvedWithTimeCount > 0
                ? Math.round((double) totalResolutionHours / resolvedWithTimeCount * 10.0) / 10.0
                : 0.0;

        Double avgSeverity = complaintsWithSeverity > 0
                ? Math.round(totalSeverity / complaintsWithSeverity * 10.0) / 10.0
                : 0.0;

        // Department breakdown
        Map<String, Long> departmentCounts = new HashMap<>();
        List<Department> departments = departmentRepository.findAll();
        for (Department dept : departments) {
            long deptTotal = complaintRepository.countByDepartmentId(dept.getId());
            departmentCounts.put(dept.getName(), deptTotal);
        }

        return HeatmapStatsResponse.builder()
                .totalComplaints(total)
                .activeComplaints(Math.max(0, activeCount))
                .resolvedComplaints(resolvedCount)
                .averageResolutionTimeHours(avgResolutionTime)
                .averageSeverityScore(avgSeverity)
                .categoryCounts(categoryCounts)
                .statusCounts(statusCounts)
                .departmentCounts(departmentCounts)
                .build();
    }

    private List<Complaint> queryComplaints(
            Double minLat, Double maxLat, Double minLon, Double maxLon,
            IssueCategory category, Integer days) {

        Instant startDate = (days != null && days > 0)
                ? Instant.now().minus(days, ChronoUnit.DAYS)
                : null;

        Specification<Complaint> spec = ComplaintSpecification.withFilters(
                category, null, null,
                minLat, maxLat, minLon, maxLon,
                startDate, null, null, null
        );

        return complaintRepository.findAll(spec);
    }
}
