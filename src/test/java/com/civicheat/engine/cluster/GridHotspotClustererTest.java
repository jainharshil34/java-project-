package com.civicheat.engine.cluster;

import com.civicheat.entity.Complaint;
import com.civicheat.enums.IssueCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GridHotspotClustererTest {

    private GridHotspotClusterer clusterer;

    @BeforeEach
    void setUp() {
        clusterer = new GridHotspotClusterer();
    }

    @Test
    @DisplayName("Empty list returns empty hotspots")
    void cluster_emptyList_returnsEmpty() {
        List<Hotspot> result = clusterer.cluster(Collections.emptyList(), 200.0);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Cluster multiple complaints in the same geographical cell")
    void cluster_sameCellComplaints_aggregatesCorrectly() {
        double baseLat = 37.774500;
        double baseLng = -122.416500;

        Complaint c1 = Complaint.builder()
                .id(UUID.randomUUID())
                .latitude(baseLat)
                .longitude(baseLng)
                .category(IssueCategory.POTHOLE)
                .severityScore(60)
                .build();

        Complaint c2 = Complaint.builder()
                .id(UUID.randomUUID())
                .latitude(baseLat + 0.0001) // ~11 meters
                .longitude(baseLng)
                .category(IssueCategory.POTHOLE)
                .severityScore(80)
                .build();

        Complaint c3 = Complaint.builder()
                .id(UUID.randomUUID())
                .latitude(baseLat + 0.0002) // ~22 meters
                .longitude(baseLng)
                .category(IssueCategory.GARBAGE)
                .severityScore(40)
                .build();

        List<Hotspot> hotspots = clusterer.cluster(List.of(c1, c2, c3), 200.0);

        assertThat(hotspots).hasSize(1);
        Hotspot hotspot = hotspots.get(0);

        assertThat(hotspot.getCount()).isEqualTo(3);
        assertThat(hotspot.getDominantCategory()).isEqualTo(IssueCategory.POTHOLE);
        assertThat(hotspot.getAverageSeverity()).isEqualTo(60.0); // (60 + 80 + 40) / 3 = 60
        assertThat(hotspot.getHeatIntensity()).isEqualTo(1.0); // Normalized highest
        assertThat(hotspot.getComplaintIds()).containsExactlyInAnyOrder(c1.getId(), c2.getId(), c3.getId());
    }

    @Test
    @DisplayName("Rank hotspots correctly by density and severity across distinct cells")
    void cluster_multipleCells_ranksByIntensity() {
        // Cell 1: High density (4 complaints, high severity)
        double cell1Lat = 37.774500;
        double cell1Lng = -122.416500;

        Complaint a1 = Complaint.builder().id(UUID.randomUUID()).latitude(cell1Lat).longitude(cell1Lng).category(IssueCategory.WATER_LEAK).severityScore(80).build();
        Complaint a2 = Complaint.builder().id(UUID.randomUUID()).latitude(cell1Lat + 0.0001).longitude(cell1Lng).category(IssueCategory.WATER_LEAK).severityScore(90).build();
        Complaint a3 = Complaint.builder().id(UUID.randomUUID()).latitude(cell1Lat + 0.0002).longitude(cell1Lng).category(IssueCategory.WATER_LEAK).severityScore(85).build();
        Complaint a4 = Complaint.builder().id(UUID.randomUUID()).latitude(cell1Lat + 0.0003).longitude(cell1Lng).category(IssueCategory.POTHOLE).severityScore(70).build();

        // Cell 2: Low density (2 complaints, low severity, ~5 km away)
        double cell2Lat = 37.820500;
        double cell2Lng = -122.370000;

        Complaint b1 = Complaint.builder().id(UUID.randomUUID()).latitude(cell2Lat).longitude(cell2Lng).category(IssueCategory.NOISE).severityScore(20).build();
        Complaint b2 = Complaint.builder().id(UUID.randomUUID()).latitude(cell2Lat + 0.0001).longitude(cell2Lng).category(IssueCategory.NOISE).severityScore(30).build();

        List<Hotspot> hotspots = clusterer.cluster(List.of(a1, a2, a3, a4, b1, b2), 200.0);

        assertThat(hotspots).hasSize(2);

        Hotspot topHotspot = hotspots.get(0);
        Hotspot secondHotspot = hotspots.get(1);

        assertThat(topHotspot.getCount()).isEqualTo(4);
        assertThat(topHotspot.getDominantCategory()).isEqualTo(IssueCategory.WATER_LEAK);
        assertThat(topHotspot.getHeatIntensity()).isEqualTo(1.0);

        assertThat(secondHotspot.getCount()).isEqualTo(2);
        assertThat(secondHotspot.getDominantCategory()).isEqualTo(IssueCategory.NOISE);
        assertThat(secondHotspot.getHeatIntensity()).isLessThan(topHotspot.getHeatIntensity());
    }
}
