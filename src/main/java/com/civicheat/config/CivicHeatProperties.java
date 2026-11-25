package com.civicheat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * Custom application configuration properties for Civic Issue Heatmap platform.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "civicheat")
public class CivicHeatProperties {

    private DuplicateProperties duplicate = new DuplicateProperties();
    private SeverityProperties severity = new SeverityProperties();
    private RoutingProperties routing = new RoutingProperties();
    private UploadProperties upload = new UploadProperties();
    private JwtProperties jwt = new JwtProperties();
    private SeederProperties seeder = new SeederProperties();
    private RateLimitProperties rateLimit = new RateLimitProperties();

    @Getter
    @Setter
    public static class SeederProperties {
        private boolean enabled = true;
        private double centerLat = 28.6139;
        private double centerLng = 77.2090;
        private double spreadKm = 10.0;
        private int count = 300;
    }

    @Getter
    @Setter
    public static class RateLimitProperties {
        private boolean enabled = true;
        private int maxRequestsPerMinute = 30;
    }

    @Getter
    @Setter
    public static class DuplicateProperties {
        private double radiusMeters = 50.0;
        private int windowDays = 14;
        private double threshold = 0.65;
        private double distanceWeight = 0.6;
        private double textWeight = 0.4;
    }

    @Getter
    @Setter
    public static class SeverityProperties {
        private double densityRadiusMeters = 200.0;
        private Set<String> keywords = new HashSet<>(Set.of(
                "accident", "flooding", "dangerous", "children", "hospital",
                "school", "fire", "injury", "hazard", "elderly", "urgent", "emergency"
        ));
        private int pointsPerUpvote = 2;
        private int maxUpvoteBoost = 20;
        private int pointsPerNearbyComplaint = 3;
        private int maxDensityBoost = 20;
        private int pointsPerKeyword = 5;
        private int maxKeywordBoost = 20;
        private int pointsPerDayOpen = 2;
        private int maxAgeBoost = 20;
    }

    @Getter
    @Setter
    public static class RoutingProperties {
        /**
         * Fallback department name if a specific issue category has no assigned mapping.
         */
        private String defaultDepartmentName = "Roads & Infrastructure";
        private String defaultDepartmentCode = "ROADS_INFRA";
    }

    @Getter
    @Setter
    public static class UploadProperties {
        private String dir = "./uploads";
    }

    @Getter
    @Setter
    public static class JwtProperties {
        private String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        private long expiryHours = 24;
    }
}
