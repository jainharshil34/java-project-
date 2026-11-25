package com.civicheat.engine.severity;

import com.civicheat.config.CivicHeatProperties;
import com.civicheat.entity.Complaint;
import com.civicheat.enums.ComplaintStatus;
import com.civicheat.enums.IssueCategory;
import com.civicheat.enums.SeverityLevel;
import com.civicheat.repository.ComplaintRepository;
import com.civicheat.util.GeoUtils;
import com.civicheat.util.TextSimilarityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Weighted multi-factor severity calculation engine.
 * Computes severity score from category base weight, community signals, local density, keywords, and incident age.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeightedSeverityCalculator implements SeverityCalculator {

    private static final Map<IssueCategory, Integer> CATEGORY_BASE_SCORES = Map.of(
            IssueCategory.WATER_LEAK, 40,
            IssueCategory.POTHOLE, 35,
            IssueCategory.STREETLIGHT, 30,
            IssueCategory.GARBAGE, 25,
            IssueCategory.NOISE, 15
    );

    private final ComplaintRepository complaintRepository;
    private final CivicHeatProperties properties;
    private final com.civicheat.config.SpatialDatabaseDetector spatialDetector;

    @Override
    public SeverityResult calculate(Complaint complaint) {
        if (complaint == null) {
            return new SeverityResult(0, SeverityLevel.LOW, Map.of());
        }

        CivicHeatProperties.SeverityProperties config = properties.getSeverity();

        // 1. Category Base Score
        int categoryBase = CATEGORY_BASE_SCORES.getOrDefault(complaint.getCategory(), 20);

        // 2. Upvote Boost
        int upvotes = complaint.getUpvoteCount() != null ? Math.max(0, complaint.getUpvoteCount()) : 0;
        int upvoteBoost = Math.min(upvotes * config.getPointsPerUpvote(), config.getMaxUpvoteBoost());

        // 3. Density Boost (open complaints of any category within density radius)
        long nearbyCount = getNearbyOpenComplaintsCount(complaint, config.getDensityRadiusMeters());
        int densityBoost = (int) Math.min(nearbyCount * config.getPointsPerNearbyComplaint(), (long) config.getMaxDensityBoost());

        // 4. Keyword Boost
        String searchableText = (complaint.getTitle() != null ? complaint.getTitle() : "") + " " +
                (complaint.getDescription() != null ? complaint.getDescription() : "");
        Set<String> tokens = TextSimilarityUtils.tokenize(searchableText);

        List<String> matchedKeywords = new ArrayList<>();
        for (String keyword : config.getKeywords()) {
            if (tokens.contains(keyword.toLowerCase(Locale.ROOT))) {
                matchedKeywords.add(keyword);
            }
        }
        int keywordBoost = Math.min(matchedKeywords.size() * config.getPointsPerKeyword(), config.getMaxKeywordBoost());

        // 5. Age Boost
        long daysOpen = 0;
        if (complaint.getCreatedAt() != null) {
            daysOpen = Math.max(0, Duration.between(complaint.getCreatedAt(), Instant.now()).toDays());
        }
        int ageBoost = (int) Math.min(daysOpen * config.getPointsPerDayOpen(), (long) config.getMaxAgeBoost());

        // Composite Score calculation and clamping [0, 100]
        int rawScore = categoryBase + upvoteBoost + densityBoost + keywordBoost + ageBoost;
        int finalScore = Math.max(0, Math.min(100, rawScore));

        SeverityLevel level = mapScoreToLevel(finalScore);

        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("categoryBase", categoryBase);
        breakdown.put("upvoteBoost", upvoteBoost);
        breakdown.put("upvotesCount", upvotes);
        breakdown.put("densityBoost", densityBoost);
        breakdown.put("nearbyOpenComplaints", nearbyCount);
        breakdown.put("keywordBoost", keywordBoost);
        breakdown.put("matchedKeywords", matchedKeywords);
        breakdown.put("ageBoost", ageBoost);
        breakdown.put("daysOpen", daysOpen);
        breakdown.put("totalScore", finalScore);

        log.debug("Calculated severity for complaint {}: raw={}, final={}, level={}, breakdown={}",
                complaint.getId(), rawScore, finalScore, level, breakdown);

        return SeverityResult.builder()
                .score(finalScore)
                .level(level)
                .breakdown(breakdown)
                .build();
    }

    /**
     * Maps numerical score (0-100) to discrete SeverityLevel.
     */
    public static SeverityLevel mapScoreToLevel(int score) {
        if (score >= 75) {
            return SeverityLevel.CRITICAL;
        } else if (score >= 50) {
            return SeverityLevel.HIGH;
        } else if (score >= 25) {
            return SeverityLevel.MEDIUM;
        } else {
            return SeverityLevel.LOW;
        }
    }

    private long getNearbyOpenComplaintsCount(Complaint complaint, double radiusMeters) {
        if (complaint.getLatitude() == null || complaint.getLongitude() == null) {
            return 0;
        }
        if (spatialDetector != null && spatialDetector.isPostgisSupported()) {
            try {
                return complaintRepository.countOpenComplaintsNearby(
                        complaint.getLatitude(),
                        complaint.getLongitude(),
                        radiusMeters,
                        complaint.getId()
                );
            } catch (Exception ex) {
                log.warn("PostGIS countOpenComplaintsNearby query failed: {}", ex.getMessage());
            }
        }
        // In-memory spatial filter fallback for H2 or non-PostGIS test DB
        List<ComplaintStatus> excluded = List.of(
                ComplaintStatus.RESOLVED,
                ComplaintStatus.REJECTED,
                ComplaintStatus.DUPLICATE
        );
        return complaintRepository.findByStatusNotIn(excluded).stream()
                .filter(c -> !c.getId().equals(complaint.getId()))
                .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
                .filter(c -> GeoUtils.haversineDistance(
                        complaint.getLatitude(), complaint.getLongitude(),
                        c.getLatitude(), c.getLongitude()) <= radiusMeters)
                .count();
    }
}
