package com.civicheat.engine.duplicate;

import com.civicheat.config.CivicHeatProperties;
import com.civicheat.dto.request.ComplaintCreateRequest;
import com.civicheat.entity.Complaint;
import com.civicheat.enums.ComplaintStatus;
import com.civicheat.enums.IssueCategory;
import com.civicheat.enums.SeverityLevel;
import com.civicheat.repository.ComplaintRepository;
import com.civicheat.util.GeoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeoTextDuplicateDetectorTest {

    @Mock
    private ComplaintRepository complaintRepository;

    private CivicHeatProperties properties;
    private GeoTextDuplicateDetector duplicateDetector;

    @BeforeEach
    void setUp() {
        properties = new CivicHeatProperties();
        properties.getDuplicate().setRadiusMeters(50.0);
        properties.getDuplicate().setWindowDays(14);
        properties.getDuplicate().setThreshold(0.65);
        properties.getDuplicate().setDistanceWeight(0.6);
        properties.getDuplicate().setTextWeight(0.4);

        duplicateDetector = new GeoTextDuplicateDetector(complaintRepository, properties, new com.civicheat.config.SpatialDatabaseDetector(true));
    }

    @Test
    @DisplayName("Duplicate detected for identical location and identical text within same category")
    void findDuplicate_sameSpotSameText_returnsDuplicate() {
        double lat = 37.7749;
        double lon = -122.4194;

        Complaint existing = Complaint.builder()
                .id(UUID.randomUUID())
                .title("Big pothole on Main St")
                .description("Deep pothole causing vehicle damage near intersection")
                .category(IssueCategory.POTHOLE)
                .latitude(lat)
                .longitude(lon)
                .location(GeoUtils.createPoint(lat, lon))
                .status(ComplaintStatus.SUBMITTED)
                .severityLevel(SeverityLevel.MEDIUM)
                .severityScore(50)
                .upvoteCount(2)
                .createdAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .build();

        when(complaintRepository.findDuplicateCandidates(eq(lat), eq(lon), eq(50.0), eq("POTHOLE"), any(Instant.class)))
                .thenReturn(List.of(existing));

        ComplaintCreateRequest req = ComplaintCreateRequest.builder()
                .title("Big pothole on Main St")
                .description("Deep pothole causing vehicle damage near intersection")
                .category(IssueCategory.POTHOLE)
                .latitude(lat)
                .longitude(lon)
                .build();

        Optional<Complaint> duplicate = duplicateDetector.findDuplicate(req);

        assertThat(duplicate).isPresent();
        assertThat(duplicate.get().getId()).isEqualTo(existing.getId());
    }

    @Test
    @DisplayName("No duplicate found when categories differ (repository yields no candidates)")
    void findDuplicate_sameSpotDifferentCategory_returnsEmpty() {
        double lat = 37.7749;
        double lon = -122.4194;

        when(complaintRepository.findDuplicateCandidates(eq(lat), eq(lon), eq(50.0), eq("GARBAGE"), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        ComplaintCreateRequest req = ComplaintCreateRequest.builder()
                .title("Garbage pile")
                .description("Huge garbage pile on roadside")
                .category(IssueCategory.GARBAGE)
                .latitude(lat)
                .longitude(lon)
                .build();

        Optional<Complaint> duplicate = duplicateDetector.findDuplicate(req);

        assertThat(duplicate).isEmpty();
    }

    @Test
    @DisplayName("No duplicate found when locations are far apart (> 50m)")
    void findDuplicate_farApart_returnsEmpty() {
        double lat1 = 37.7749;
        double lon1 = -122.4194;

        // ~1 km away
        double lat2 = 37.7840;
        double lon2 = -122.4194;

        Complaint existing = Complaint.builder()
                .id(UUID.randomUUID())
                .title("Pothole on Main St")
                .description("Pothole near signal")
                .category(IssueCategory.POTHOLE)
                .latitude(lat2)
                .longitude(lon2)
                .location(GeoUtils.createPoint(lat2, lon2))
                .status(ComplaintStatus.SUBMITTED)
                .build();

        // Repository returns candidate but distance calculation yields > 50m
        when(complaintRepository.findDuplicateCandidates(eq(lat1), eq(lon1), eq(50.0), eq("POTHOLE"), any(Instant.class)))
                .thenReturn(List.of(existing));

        ComplaintCreateRequest req = ComplaintCreateRequest.builder()
                .title("Pothole on Main St")
                .description("Pothole near signal")
                .category(IssueCategory.POTHOLE)
                .latitude(lat1)
                .longitude(lon1)
                .build();

        Optional<Complaint> duplicate = duplicateDetector.findDuplicate(req);

        assertThat(duplicate).isEmpty();
    }

    @Test
    @DisplayName("Old complaint outside window (e.g. >14 days) is not returned by query")
    void findDuplicate_outsideWindow_returnsEmpty() {
        double lat = 37.7749;
        double lon = -122.4194;

        when(complaintRepository.findDuplicateCandidates(anyDouble(), anyDouble(), anyDouble(), anyString(), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        ComplaintCreateRequest req = ComplaintCreateRequest.builder()
                .title("Streetlight out")
                .description("Streetlight has been broken for months")
                .category(IssueCategory.STREETLIGHT)
                .latitude(lat)
                .longitude(lon)
                .build();

        Optional<Complaint> duplicate = duplicateDetector.findDuplicate(req);

        assertThat(duplicate).isEmpty();
    }

    @Test
    @DisplayName("Borderline threshold below 0.65 should NOT trigger duplicate")
    void findDuplicate_borderlineBelowThreshold_returnsEmpty() {
        double lat1 = 37.774900;
        double lon1 = -122.419400;

        // ~25m away -> distanceScore = (1 - 25/50) = 0.50 -> distancePart = 0.50 * 0.6 = 0.30
        // Text: slight overlap -> textScore ~ 0.50 -> textPart = 0.50 * 0.4 = 0.20
        // Composite = 0.50 < 0.65 threshold
        double lat2 = 37.775125;
        double lon2 = -122.419400;

        Complaint candidate = Complaint.builder()
                .id(UUID.randomUUID())
                .title("Water pipeline problem")
                .description("Water is leaking from pipe")
                .category(IssueCategory.WATER_LEAK)
                .latitude(lat2)
                .longitude(lon2)
                .location(GeoUtils.createPoint(lat2, lon2))
                .status(ComplaintStatus.SUBMITTED)
                .build();

        when(complaintRepository.findDuplicateCandidates(eq(lat1), eq(lon1), eq(50.0), eq("WATER_LEAK"), any(Instant.class)))
                .thenReturn(List.of(candidate));

        ComplaintCreateRequest req = ComplaintCreateRequest.builder()
                .title("Flooding in street")
                .description("Excessive drainage overflow and blockage")
                .category(IssueCategory.WATER_LEAK)
                .latitude(lat1)
                .longitude(lon1)
                .build();

        Optional<Complaint> duplicate = duplicateDetector.findDuplicate(req);

        assertThat(duplicate).isEmpty();
    }

    @Test
    @DisplayName("Borderline threshold above 0.65 SHOULD trigger duplicate")
    void findDuplicate_borderlineAboveThreshold_returnsDuplicate() {
        double lat1 = 37.774900;
        double lon1 = -122.419400;

        // ~10m away -> distanceScore = (1 - 10/50) = 0.80 -> distancePart = 0.80 * 0.6 = 0.48
        // Text: similar words -> textScore = 0.60 -> textPart = 0.60 * 0.4 = 0.24
        // Composite = 0.72 >= 0.65 threshold -> MATCH!
        double lat2 = 37.774990;
        double lon2 = -122.419400;

        Complaint candidate = Complaint.builder()
                .id(UUID.randomUUID())
                .title("Overflowing garbage bin corner")
                .description("Municipal waste bin overflowing on sidewalk")
                .category(IssueCategory.GARBAGE)
                .latitude(lat2)
                .longitude(lon2)
                .location(GeoUtils.createPoint(lat2, lon2))
                .status(ComplaintStatus.SUBMITTED)
                .build();

        when(complaintRepository.findDuplicateCandidates(eq(lat1), eq(lon1), eq(50.0), eq("GARBAGE"), any(Instant.class)))
                .thenReturn(List.of(candidate));

        ComplaintCreateRequest req = ComplaintCreateRequest.builder()
                .title("Garbage bin overflowing")
                .description("Municipal waste bin overflowing on the street")
                .category(IssueCategory.GARBAGE)
                .latitude(lat1)
                .longitude(lon1)
                .build();

        Optional<Complaint> duplicate = duplicateDetector.findDuplicate(req);

        assertThat(duplicate).isPresent();
        assertThat(duplicate.get().getId()).isEqualTo(candidate.getId());
    }
}
