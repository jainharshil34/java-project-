package com.civicheat.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.locationtech.jts.geom.Point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class GeoUtilsTest {

    @Test
    @DisplayName("Haversine distance between same point should be 0")
    void haversineDistance_samePoint_returnsZero() {
        double lat = 37.7749;
        double lon = -122.4194;

        double distance = GeoUtils.haversineDistance(lat, lon, lat, lon);
        assertThat(distance).isCloseTo(0.0, within(0.001));
    }

    @Test
    @DisplayName("Haversine distance between London and Paris (~343.5 km)")
    void haversineDistance_londonToParis_returnsAccurateDistance() {
        // London: 51.5074 N, 0.1278 W
        double londonLat = 51.5074;
        double londonLon = -0.1278;

        // Paris: 48.8566 N, 2.3522 E
        double parisLat = 48.8566;
        double parisLon = 2.3522;

        double distance = GeoUtils.haversineDistance(londonLat, londonLon, parisLat, parisLon);

        // Expected distance is ~343.5 km (+/- 1 km tolerance for spherical vs ellipsoidal model)
        assertThat(distance).isCloseTo(343556.0, within(1500.0));
    }

    @Test
    @DisplayName("Haversine distance between New York and Washington D.C. (~328 km)")
    void haversineDistance_nycToDc_returnsAccurateDistance() {
        double nycLat = 40.7128;
        double nycLon = -74.0060;

        double dcLat = 38.9072;
        double dcLon = -77.0369;

        double distance = GeoUtils.haversineDistance(nycLat, nycLon, dcLat, dcLon);
        assertThat(distance).isCloseTo(328000.0, within(2000.0));
    }

    @ParameterizedTest
    @CsvSource({
            "0.0, 0.0, true",
            "51.5, -0.1, true",
            "90.0, 180.0, true",
            "-90.0, -180.0, true",
            "91.0, 0.0, false",
            "-90.5, 0.0, false",
            "0.0, 180.5, false",
            "0.0, -181.0, false"
    })
    @DisplayName("Validate geographical coordinates boundary cases")
    void isValidCoordinate_variousCoordinates(double lat, double lon, boolean expected) {
        assertThat(GeoUtils.isValidCoordinate(lat, lon)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Calculate bounding box contains the center coordinate")
    void calculateBoundingBox_containsCenter() {
        double lat = 12.9716;
        double lon = 77.5946;
        double radiusMeters = 500.0;

        GeoUtils.BoundingBox box = GeoUtils.calculateBoundingBox(lat, lon, radiusMeters);

        assertThat(box.minLat()).isLessThan(lat);
        assertThat(box.maxLat()).isGreaterThan(lat);
        assertThat(box.minLon()).isLessThan(lon);
        assertThat(box.maxLon()).isGreaterThan(lon);
        assertThat(box.contains(lat, lon)).isTrue();
    }

    @Test
    @DisplayName("Create JTS Point creates valid geometry with SRID 4326")
    void createPoint_createsPointWithCorrectSRID() {
        double lat = 28.6139;
        double lon = 77.2090;

        Point point = GeoUtils.createPoint(lat, lon);

        assertThat(point).isNotNull();
        assertThat(point.getSRID()).isEqualTo(4326);
        assertThat(point.getX()).isEqualTo(lon);
        assertThat(point.getY()).isEqualTo(lat);
    }

    @Test
    @DisplayName("Invalid coordinates throw IllegalArgumentException")
    void invalidCoordinates_throwsException() {
        assertThatThrownBy(() -> GeoUtils.haversineDistance(100.0, 0.0, 0.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
