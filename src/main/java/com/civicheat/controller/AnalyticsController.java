package com.civicheat.controller;

import com.civicheat.entity.Complaint;
import com.civicheat.enums.ComplaintStatus;
import com.civicheat.repository.ComplaintRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced analytics API: resolution rates, avg resolution time, daily trends, and breakdowns.
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Advanced statistics and trend analytics")
public class AnalyticsController {

    private final ComplaintRepository complaintRepository;

    @GetMapping("/resolution-rate")
    @Operation(summary = "Get resolution rate percentage by category")
    public ResponseEntity<Map<String, Object>> getResolutionRate() {
        List<Complaint> all = complaintRepository.findAll();
        long total = all.size();
        long resolved = all.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED)
                .count();

        Map<String, Long> byCategory = all.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED)
                .collect(Collectors.groupingBy(c -> c.getCategory().name(), Collectors.counting()));

        Map<String, Long> totalByCategory = all.stream()
                .collect(Collectors.groupingBy(c -> c.getCategory().name(), Collectors.counting()));

        Map<String, Double> rateByCategory = new LinkedHashMap<>();
        totalByCategory.forEach((cat, tot) -> {
            long res = byCategory.getOrDefault(cat, 0L);
            rateByCategory.put(cat, tot > 0 ? Math.round((res * 100.0 / tot) * 10.0) / 10.0 : 0.0);
        });

        return ResponseEntity.ok(Map.of(
            "totalComplaints", total,
            "resolvedComplaints", resolved,
            "overallResolutionRate", total > 0 ? Math.round((resolved * 100.0 / total) * 10.0) / 10.0 : 0.0,
            "resolutionRateByCategory", rateByCategory
        ));
    }

    @GetMapping("/avg-resolution-time")
    @Operation(summary = "Average resolution time in hours per category")
    public ResponseEntity<Map<String, Object>> getAvgResolutionTime() {
        List<Complaint> resolved = complaintRepository.findAll().stream()
                .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED && c.getResolvedAt() != null)
                .toList();

        Map<String, Double> avgByCategory = resolved.stream()
                .collect(Collectors.groupingBy(
                    c -> c.getCategory().name(),
                    Collectors.averagingDouble(c -> ChronoUnit.HOURS.between(c.getCreatedAt(), c.getResolvedAt()))
                ));

        double overall = resolved.stream()
                .mapToLong(c -> ChronoUnit.HOURS.between(c.getCreatedAt(), c.getResolvedAt()))
                .average().orElse(0.0);

        Map<String, Double> rounded = new LinkedHashMap<>();
        avgByCategory.forEach((k, v) -> rounded.put(k, Math.round(v * 10.0) / 10.0));

        return ResponseEntity.ok(Map.of(
            "overallAvgResolutionHours", Math.round(overall * 10.0) / 10.0,
            "avgResolutionHoursByCategory", rounded,
            "totalResolvedSamples", resolved.size()
        ));
    }

    @GetMapping("/daily-trend")
    @Operation(summary = "Daily complaint submission counts for the last N days")
    public ResponseEntity<Map<String, Object>> getDailyTrend(
            @RequestParam(defaultValue = "14") int days) {

        int safeDays = Math.max(1, Math.min(days, 90));
        Instant since = Instant.now().minus(safeDays, ChronoUnit.DAYS);
        List<Complaint> recent = complaintRepository.findAll().stream()
                .filter(c -> c.getCreatedAt().isAfter(since))
                .toList();

        Map<String, Long> dailyCounts = recent.stream()
                .collect(Collectors.groupingBy(
                    c -> c.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().toString(),
                    Collectors.counting()
                ));

        Map<String, Long> filled = new TreeMap<>();
        for (int i = safeDays - 1; i >= 0; i--) {
            String date = Instant.now().minus(i, ChronoUnit.DAYS)
                    .atZone(ZoneOffset.UTC).toLocalDate().toString();
            filled.put(date, dailyCounts.getOrDefault(date, 0L));
        }

        return ResponseEntity.ok(Map.of(
            "days", safeDays,
            "totalInPeriod", recent.size(),
            "dailyCounts", filled
        ));
    }

    @GetMapping("/status-breakdown")
    @Operation(summary = "Count of complaints per status")
    public ResponseEntity<Map<String, Long>> getStatusBreakdown() {
        List<Complaint> all = complaintRepository.findAll();
        Map<String, Long> breakdown = all.stream()
                .collect(Collectors.groupingBy(c -> c.getStatus().name(), Collectors.counting()));
        return ResponseEntity.ok(new TreeMap<>(breakdown));
    }

    @GetMapping("/severity-breakdown")
    @Operation(summary = "Count of complaints per severity level")
    public ResponseEntity<Map<String, Long>> getSeverityBreakdown() {
        List<Complaint> all = complaintRepository.findAll();
        Map<String, Long> breakdown = all.stream()
                .collect(Collectors.groupingBy(c -> c.getSeverityLevel().name(), Collectors.counting()));
        return ResponseEntity.ok(new TreeMap<>(breakdown));
    }
}
