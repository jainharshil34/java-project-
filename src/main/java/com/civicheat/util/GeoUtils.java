package com.civicheat.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Utility functions for geospatial computations, distance calculations, and coordinate validation.
 */
public final class GeoUtils {

    /** Earth's mean radius in meters. */
    public static final double EARTH_RADIUS_METERS = 6371000.0;
    public static final int SRID_WGS84 = 4326;

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    private GeoUtils() {
        // Utility class
    }

    /**
     * Validates whether latitude and longitude are within standard geographical bounds.
     *
     * @param lat Latitude in degrees [-90.0, 90.0]
     * @param lon Longitude in degrees [-180.0, 180.0]
     * @return true if coordinate is valid, false otherwise
     */
    public static boolean isValidCoordinate(double lat, double lon) {
        return !Double.isNaN(lat) && !Double.isNaN(lon)
                && lat >= -90.0 && lat <= 90.0
                && lon >= -180.0 && lon <= 180.0;
    }

    /**
     * Calculates great-circle distance between two geographical points using the Haversine formula.
     *
     * @param lat1 Latitude of point 1 in degrees
     * @param lon1 Longitude of point 1 in degrees
     * @param lat2 Latitude of point 2 in degrees
     * @param lon2 Longitude of point 2 in degrees
     * @return Great-circle distance in meters
     */
    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        if (!isValidCoordinate(lat1, lon1) || !isValidCoordinate(lat2, lon2)) {
            throw new IllegalArgumentException("Invalid latitude or longitude values provided");
        }

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Calculates a rectangular geographical bounding box around a center point for a given radius.
     *
     * @param lat          Center latitude in degrees
     * @param lon          Center longitude in degrees
     * @param radiusMeters Search radius in meters
     * @return BoundingBox containing min/max latitude and longitude
     */
    public static BoundingBox calculateBoundingBox(double lat, double lon, double radiusMeters) {
        if (!isValidCoordinate(lat, lon)) {
            throw new IllegalArgumentException("Invalid center coordinates");
        }
        if (radiusMeters < 0) {
            throw new IllegalArgumentException("Radius must be non-negative");
        }

        double radDist = radiusMeters / EARTH_RADIUS_METERS;
        double radLat = Math.toRadians(lat);
        double radLon = Math.toRadians(lon);

        double minLat = radLat - radDist;
        double maxLat = radLat + radDist;

        double minLon;
        double maxLon;

        // Check if pole is enclosed
        if (minLat > -Math.PI / 2.0 && maxLat < Math.PI / 2.0) {
            double deltaLon = Math.asin(Math.sin(radDist) / Math.cos(radLat));
            minLon = radLon - deltaLon;
            maxLon = radLon + deltaLon;

            if (minLon < -Math.PI) {
                minLon += 2.0 * Math.PI;
            }
            if (maxLon > Math.PI) {
                maxLon -= 2.0 * Math.PI;
            }
        } else {
            // Near poles
            minLat = Math.max(minLat, -Math.PI / 2.0);
            maxLat = Math.min(maxLat, Math.PI / 2.0);
            minLon = -Math.PI;
            maxLon = Math.PI;
        }

        return new BoundingBox(
                Math.toDegrees(minLat),
                Math.toDegrees(maxLat),
                Math.toDegrees(minLon),
                Math.toDegrees(maxLon)
        );
    }

    /**
     * Creates a JTS Point geometry with SRID 4326 (WGS84).
     *
     * @param lat Latitude
     * @param lon Longitude
     * @return JTS Point geometry (x = lon, y = lat)
     */
    public static Point createPoint(double lat, double lon) {
        if (!isValidCoordinate(lat, lon)) {
            throw new IllegalArgumentException("Invalid coordinates: lat=" + lat + ", lon=" + lon);
        }
        // In GIS/JTS, x = longitude, y = latitude
        return GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));
    }

    /**
     * Record representing a geographic bounding box.
     */
    public record BoundingBox(double minLat, double maxLat, double minLon, double maxLon) {
        public boolean contains(double lat, double lon) {
            return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
        }
    }
}
