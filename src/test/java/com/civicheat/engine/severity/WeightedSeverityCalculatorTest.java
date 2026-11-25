package com.civicheat.engine.severity;

import com.civicheat.config.CivicHeatProperties;
import com.civicheat.entity.Complaint;
import com.civicheat.enums.IssueCategory;
import com.civicheat.enums.SeverityLevel;
import com.civicheat.repository.ComplaintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeightedSeverityCalculatorTest {

    @Mock
    private ComplaintRepository complaintRepository;

    private CivicHeatProperties properties;
    private WeightedSeverityCalculator calculator;

    @BeforeEach
    void setUp() {
        properties = new CivicHeatProperties();
        calculator = new WeightedSeverityCalculator(complaintRepository, properties, new com.civicheat.config.SpatialDatabaseDetector(true));
    }

    @ParameterizedTest
    @CsvSource({
            "WATER_LEAK, 40, MEDIUM",
            "POTHOLE, 35, MEDIUM",
            "STREETLIGHT, 30, MEDIUM",
            "GARBAGE, 25, MEDIUM",
            "NOISE, 15, LOW"
    })
    @DisplayName("Category base weights are correctly mapped to base score and severity level")
    void calculate_categoryBaseScores(IssueCategory category, int expectedBaseScore, SeverityLevel expectedLevel) {
        Complaint complaint = Complaint.builder()
                .id(UUID.randomUUID())
                .title("Standard report")
                .description("Routine observation of municipal public utility")
                .category(category)
                .latitude(12.9716)
                .longitude(77.5946)
                .upvoteCount(0)
                .createdAt(Instant.now())
                .build();

        when(complaintRepository.countOpenComplaintsNearby(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(0L);

        SeverityResult result = calculator.calculate(complaint);

        assertThat(result.getScore()).isEqualTo(expectedBaseScore);
        assertThat(result.getLevel()).isEqualTo(expectedLevel);
        assertThat(result.getBreakdown().get("categoryBase")).isEqualTo(expectedBaseScore);
    }

    @Test
    @DisplayName("Upvotes boost severity score up to configured cap (20 pts)")
    void calculate_upvoteBoost() {
        Complaint complaint = Complaint.builder()
                .id(UUID.randomUUID())
                .title("Noise issue")
                .description("Late night party")
                .category(IssueCategory.NOISE) // Base 15
                .latitude(12.9716)
                .longitude(77.5946)
                .upvoteCount(15) // 15 * 2 = 30 -> capped at 20
                .createdAt(Instant.now())
                .build();

        when(complaintRepository.countOpenComplaintsNearby(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(0L);

        SeverityResult result = calculator.calculate(complaint);

        // 15 base + 20 upvote = 35 -> MEDIUM
        assertThat(result.getScore()).isEqualTo(35);
        assertThat(result.getLevel()).isEqualTo(SeverityLevel.MEDIUM);
        assertThat(result.getBreakdown().get("upvoteBoost")).isEqualTo(20);
    }

    @Test
    @DisplayName("Density of nearby open complaints boosts severity")
    void calculate_densityBoost() {
        Complaint complaint = Complaint.builder()
                .id(UUID.randomUUID())
                .title("Garbage pile")
                .description("Sidewalk garbage")
                .category(IssueCategory.GARBAGE) // Base 25
                .latitude(12.9716)
                .longitude(77.5946)
                .upvoteCount(0)
                .createdAt(Instant.now())
                .build();

        // 4 nearby complaints * 3 pts = 12 pts
        when(complaintRepository.countOpenComplaintsNearby(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(4L);

        SeverityResult result = calculator.calculate(complaint);

        // 25 base + 12 density = 37 -> MEDIUM
        assertThat(result.getScore()).isEqualTo(37);
        assertThat(result.getBreakdown().get("densityBoost")).isEqualTo(12);
        assertThat(result.getBreakdown().get("nearbyOpenComplaints")).isEqualTo(4L);
    }

    @Test
    @DisplayName("Critical keywords boost severity score")
    void calculate_keywordBoost() {
        Complaint complaint = Complaint.builder()
                .id(UUID.randomUUID())
                .title("Dangerous pothole near school")
                .description("Caused vehicle accident and risk to children")
                .category(IssueCategory.POTHOLE) // Base 35
                .latitude(12.9716)
                .longitude(77.5946)
                .upvoteCount(0)
                .createdAt(Instant.now())
                .build();

        when(complaintRepository.countOpenComplaintsNearby(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(0L);

        // Matched keywords: "dangerous", "school", "accident", "children" -> 4 * 5 = 20 pts
        SeverityResult result = calculator.calculate(complaint);

        // 35 base + 20 keyword = 55 -> HIGH
        assertThat(result.getScore()).isEqualTo(55);
        assertThat(result.getLevel()).isEqualTo(SeverityLevel.HIGH);
        assertThat(result.getBreakdown().get("keywordBoost")).isEqualTo(20);
    }

    @Test
    @DisplayName("Age of unresolved complaint increases severity points per day")
    void calculate_ageBoost() {
        Complaint complaint = Complaint.builder()
                .id(UUID.randomUUID())
                .title("Broken streetlight")
                .description("Dark corner")
                .category(IssueCategory.STREETLIGHT) // Base 30
                .latitude(12.9716)
                .longitude(77.5946)
                .upvoteCount(0)
                .createdAt(Instant.now().minus(6, ChronoUnit.DAYS)) // 6 days * 2 = 12 pts
                .build();

        when(complaintRepository.countOpenComplaintsNearby(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(0L);

        SeverityResult result = calculator.calculate(complaint);

        // 30 base + 12 age = 42 -> MEDIUM
        assertThat(result.getScore()).isEqualTo(42);
        assertThat(result.getBreakdown().get("ageBoost")).isEqualTo(12);
    }

    @Test
    @DisplayName("Multiple high-risk factors trigger CRITICAL severity level (75+ pts)")
    void calculate_criticalSeverity() {
        Complaint complaint = Complaint.builder()
                .id(UUID.randomUUID())
                .title("Severe flooding near hospital")
                .description("Dangerous water pipeline burst causing road hazard and emergency")
                .category(IssueCategory.WATER_LEAK) // Base 40
                .latitude(12.9716)
                .longitude(77.5946)
                .upvoteCount(10) // 10 * 2 = 20 pts
                .createdAt(Instant.now().minus(4, ChronoUnit.DAYS)) // 4 * 2 = 8 pts
                .build();

        // 3 nearby complaints * 3 = 9 pts
        when(complaintRepository.countOpenComplaintsNearby(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(3L);

        // Keywords: "flooding", "hospital", "dangerous", "hazard", "emergency" -> 5 * 5 = 25 -> capped at 20
        // Total = 40 (base) + 20 (upvote) + 9 (density) + 20 (keyword) + 8 (age) = 97
        SeverityResult result = calculator.calculate(complaint);

        assertThat(result.getScore()).isEqualTo(97);
        assertThat(result.getLevel()).isEqualTo(SeverityLevel.CRITICAL);
    }

    @Test
    @DisplayName("Total severity score is clamped to maximum 100")
    void calculate_scoreClamping() {
        Complaint complaint = Complaint.builder()
                .id(UUID.randomUUID())
                .title("Emergency flooding disaster near hospital and school")
                .description("Dangerous accident and fire hazard with children and elderly injured")
                .category(IssueCategory.WATER_LEAK) // Base 40
                .latitude(12.9716)
                .longitude(77.5946)
                .upvoteCount(50) // capped 20
                .createdAt(Instant.now().minus(30, ChronoUnit.DAYS)) // capped 20
                .build();

        when(complaintRepository.countOpenComplaintsNearby(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(20L); // capped 20

        SeverityResult result = calculator.calculate(complaint);

        // 40 + 20 + 20 + 20 + 20 = 120 -> clamped to 100
        assertThat(result.getScore()).isEqualTo(100);
        assertThat(result.getLevel()).isEqualTo(SeverityLevel.CRITICAL);
    }
}
