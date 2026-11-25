package com.civicheat.engine.duplicate;

import com.civicheat.config.CivicHeatProperties;
import com.civicheat.dto.request.ComplaintCreateRequest;
import com.civicheat.entity.Complaint;
import com.civicheat.repository.ComplaintRepository;
import com.civicheat.util.GeoUtils;
import com.civicheat.util.TextSimilarityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spatial and textual similarity duplicate detection strategy.
 * Evaluates geographic proximity (weighted 0.6) and Jaccard token similarity (weighted 0.4).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeoTextDuplicateDetector implements DuplicateDetector {

    private final ComplaintRepository complaintRepository;
    private final CivicHeatProperties properties;
    private final com.civicheat.config.SpatialDatabaseDetector spatialDetector;

    @Override
    public Optional<Complaint> findDuplicate(ComplaintCreateRequest req) {
        if (req == null || req.getCategory() == null || req.getLatitude() == null || req.getLongitude() == null) {
            return Optional.empty();
        }

        CivicHeatProperties.DuplicateProperties config = properties.getDuplicate();
        double radiusMeters = config.getRadiusMeters();
        int windowDays = config.getWindowDays();
        double threshold = config.getThreshold();
        double distanceWeight = config.getDistanceWeight();
        double textWeight = config.getTextWeight();

        Instant sinceDate = Instant.now().minus(Duration.ofDays(windowDays));

        List<Complaint> candidates;
        if (spatialDetector != null && spatialDetector.isPostgisSupported()) {
            try {
                candidates = complaintRepository.findDuplicateCandidates(
                        req.getLatitude(),
                        req.getLongitude(),
                        radiusMeters,
                        req.getCategory().name(),
                        sinceDate
                );
            } catch (Exception ex) {
                log.warn("PostGIS findDuplicateCandidates query failed: {}", ex.getMessage());
                candidates = findDuplicateCandidatesInMemory(req, radiusMeters, sinceDate);
            }
        } else {
            candidates = findDuplicateCandidatesInMemory(req, radiusMeters, sinceDate);
        }

        if (candidates.isEmpty()) {
            log.debug("No duplicate candidates found within {}m for category {}", radiusMeters, req.getCategory());
            return Optional.empty();
        }

        Complaint bestMatch = null;
        double bestScore = -1.0;

        String reqText = (req.getTitle() != null ? req.getTitle() : "") + " " +
                (req.getDescription() != null ? req.getDescription() : "");

        for (Complaint candidate : candidates) {
            double distance = GeoUtils.haversineDistance(
                    req.getLatitude(), req.getLongitude(),
                    candidate.getLatitude(), candidate.getLongitude()
            );

            double distanceScore = distance <= radiusMeters
                    ? Math.max(0.0, 1.0 - (distance / radiusMeters))
                    : 0.0;

            String candidateText = (candidate.getTitle() != null ? candidate.getTitle() : "") + " " +
                    (candidate.getDescription() != null ? candidate.getDescription() : "");

            double textScore = TextSimilarityUtils.jaccardSimilarity(reqText, candidateText);

            double compositeScore = (distanceScore * distanceWeight) + (textScore * textWeight);

            log.debug("Evaluating duplicate candidate ID {}: distance={:.2f}m, distScore={:.3f}, textScore={:.3f}, composite={:.3f}",
                    candidate.getId(), distance, distanceScore, textScore, compositeScore);

            if (compositeScore > bestScore) {
                bestScore = compositeScore;
                bestMatch = candidate;
            }
        }

        if (bestMatch != null && bestScore >= threshold) {
            log.info("Duplicate detected! Incoming complaint matches existing complaint {} with composite score {:.3f} (threshold: {})",
                    bestMatch.getId(), bestScore, threshold);
            return Optional.of(bestMatch);
        }

        log.debug("Highest candidate score was {:.3f} which is below duplicate threshold {}", bestScore, threshold);
        return Optional.empty();
    }

    private List<Complaint> findDuplicateCandidatesInMemory(ComplaintCreateRequest req, double radiusMeters, Instant sinceDate) {
        List<com.civicheat.enums.ComplaintStatus> excluded = List.of(
                com.civicheat.enums.ComplaintStatus.RESOLVED,
                com.civicheat.enums.ComplaintStatus.REJECTED,
                com.civicheat.enums.ComplaintStatus.DUPLICATE
        );
        return complaintRepository.findByCategoryAndCreatedAtAfterAndStatusNotIn(
                req.getCategory(), sinceDate, excluded
        ).stream()
        .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
        .filter(c -> GeoUtils.haversineDistance(req.getLatitude(), req.getLongitude(), c.getLatitude(), c.getLongitude()) <= radiusMeters)
        .toList();
    }
}
