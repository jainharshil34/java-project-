package com.civicheat.config;

import com.civicheat.entity.Complaint;
import com.civicheat.entity.ComplaintStatusHistory;
import com.civicheat.entity.Department;
import com.civicheat.entity.User;
import com.civicheat.enums.ComplaintStatus;
import com.civicheat.enums.IssueCategory;
import com.civicheat.enums.Role;
import com.civicheat.enums.SeverityLevel;
import com.civicheat.repository.ComplaintRepository;
import com.civicheat.repository.DepartmentRepository;
import com.civicheat.repository.UserRepository;
import com.civicheat.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generates ~300 realistic geotagged civic issue incidents clustered around a configurable city center for dev demo purposes.
 */
@Slf4j
@Component
@Profile({"dev", "local"})
@Order(2)
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final ComplaintRepository complaintRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final CivicHeatProperties properties;
    private final PasswordEncoder passwordEncoder;

    private static final String[] POTHOLE_TITLES = {
            "Deep asphalt pothole damaging vehicle rims near Connaught Place Outer Circle",
            "Craters on pedestrian crossing near AIIMS Hospital Ring Road",
            "Severe road sinkhole after monsoon rain near Karol Bagh Metro",
            "Broken road pavement causing dangerous traffic hazard on Vikas Marg",
            "Multiple potholes on bicycle and bus lane near Nehru Place"
    };

    private static final String[] GARBAGE_TITLES = {
            "Overflowing public waste bin attracting stray animals near Chandni Chowk",
            "Illegal trash dumping on sidewalk near Lajpat Nagar Central Market",
            "Uncollected domestic garbage pile near Saket District Centre",
            "Hazardous construction debris blocking roadway near Rohini Sector 14",
            "Foul smelling waste accumulation near Hauz Khas market"
    };

    private static final String[] STREETLIGHT_TITLES = {
            "Flickering streetlight creating dangerous dark corner on Ring Road",
            "Completely dark street corner near India Gate pedestrian walkway",
            "Broken street pole with exposed electrical wiring near Janpath",
            "Outage of entire block streetlamps near Rajiv Chowk Metro station",
            "Dim streetlight creating high accident risk at Dhaula Kuan junction"
    };

    private static final String[] WATER_LEAK_TITLES = {
            "High pressure Delhi Jal Board water pipe burst flooding road near ITO",
            "Subsurface main water pipe leakage causing sinkhole near South Extension",
            "Broken drinking water pipeline spraying into traffic on MG Road",
            "Sewage overflow contamination near Dwarka Sector 10 school",
            "Persistent clean water pipe leak flooding residential lane in Greater Kailash"
    };

    private static final String[] NOISE_TITLES = {
            "Excessive late night commercial construction noise near Defence Colony",
            "Continuous high decibel industrial generator noise near Okhla Phase 3",
            "Illegal public loudspeaker broadcast after midnight near Chandni Chowk",
            "Unregulated heavy vehicle transit noise near Safdarjung Hospital zone",
            "Loud vibration and acoustic pollution near Mayapuri Industrial Area"
    };

    @Override
    public void run(String... args) {
        if (!properties.getSeeder().isEnabled()) {
            return;
        }

        long existingCount = complaintRepository.count();
        if (existingCount >= 10) {
            log.info("Database already populated with {} complaints. Skipping demo seeder.", existingCount);
            return;
        }

        log.info("Seeding realistic civic incidents around city center ({}, {})...",
                properties.getSeeder().getCenterLat(), properties.getSeeder().getCenterLng());

        // 1. Ensure Demo Citizen User
        User demoCitizen = userRepository.findByEmail("citizen.demo@civicheat.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("citizen.demo@civicheat.local")
                        .password(passwordEncoder.encode("Password123!"))
                        .fullName("Civic Citizen")
                        .phone("+1-555-0199")
                        .role(Role.CITIZEN)
                        .build()));

        // 2. Fetch or create departments
        Map<IssueCategory, Department> deptMap = getOrCreateDepartments();

        // 3. Define 6 dense cluster centers to form realistic hotspots
        double centerLat = properties.getSeeder().getCenterLat();
        double centerLng = properties.getSeeder().getCenterLng();

        double[][] clusterCenters = {
                {centerLat, centerLng},                               // Downtown Central
                {centerLat + 0.018, centerLng + 0.015},               // North-East District
                {centerLat - 0.022, centerLng + 0.010},               // South Market Zone
                {centerLat + 0.012, centerLng - 0.025},               // West Residential Park
                {centerLat - 0.015, centerLng - 0.018},               // South-West Industrial
                {centerLat + 0.005, centerLng - 0.008}                // Midtown Hospital Zone
        };

        Random random = new Random(42); // Deterministic seed for reproducible demo clusters
        List<Complaint> complaintsToSave = new ArrayList<>();
        int targetTotal = properties.getSeeder().getCount();

        for (int i = 0; i < targetTotal; i++) {
            // Pick a cluster center (80% clustered, 20% random scatter)
            double lat;
            double lng;
            if (random.nextDouble() < 0.8) {
                int clusterIdx = random.nextInt(clusterCenters.length);
                double[] cluster = clusterCenters[clusterIdx];
                // Gaussian scatter within ~300m
                lat = cluster[0] + (random.nextGaussian() * 0.0025);
                lng = cluster[1] + (random.nextGaussian() * 0.0025);
            } else {
                lat = centerLat + ((random.nextDouble() - 0.5) * 0.07);
                lng = centerLng + ((random.nextDouble() - 0.5) * 0.07);
            }

            // Pick Category
            IssueCategory category = IssueCategory.values()[random.nextInt(IssueCategory.values().length)];
            Department department = deptMap.getOrDefault(category, deptMap.get(IssueCategory.POTHOLE));

            String title = pickTitle(category, random);
            String desc = title + " reported by citizen. Emergency hazard identified near pedestrian area requiring urgent municipal inspection.";

            int severityScore = 20 + random.nextInt(75);
            SeverityLevel level = severityScore >= 75 ? SeverityLevel.CRITICAL :
                                  severityScore >= 50 ? SeverityLevel.HIGH :
                                  severityScore >= 25 ? SeverityLevel.MEDIUM : SeverityLevel.LOW;

            ComplaintStatus status = pickStatus(random);
            Instant createdAt = Instant.now().minus(random.nextInt(20), ChronoUnit.DAYS).minus(random.nextInt(24), ChronoUnit.HOURS);
            Instant resolvedAt = (status == ComplaintStatus.RESOLVED) ? createdAt.plus(random.nextInt(48) + 4, ChronoUnit.HOURS) : null;

            Complaint complaint = Complaint.builder()
                    .title(title)
                    .description(desc)
                    .category(category)
                    .latitude(Math.round(lat * 1000000.0) / 1000000.0)
                    .longitude(Math.round(lng * 1000000.0) / 1000000.0)
                    .location(GeoUtils.createPoint(lat, lng))
                    .severityScore(severityScore)
                    .severityLevel(level)
                    .status(status)
                    .upvoteCount(random.nextInt(15))
                    .reporter(demoCitizen)
                    .department(department)
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .resolvedAt(resolvedAt)
                    .build();

            ComplaintStatusHistory history = ComplaintStatusHistory.builder()
                    .complaint(complaint)
                    .previousStatus(null)
                    .newStatus(status)
                    .changedBy(demoCitizen)
                    .remarks("Initial submission and routing to " + department.getName())
                    .createdAt(createdAt)
                    .build();

            complaint.getStatusHistories().add(history);
            complaintsToSave.add(complaint);
        }

        complaintRepository.saveAll(complaintsToSave);
        log.info("Successfully seeded {} realistic demo civic complaints.", complaintsToSave.size());
    }

    private String pickTitle(IssueCategory cat, Random rand) {
        return switch (cat) {
            case POTHOLE -> POTHOLE_TITLES[rand.nextInt(POTHOLE_TITLES.length)];
            case GARBAGE -> GARBAGE_TITLES[rand.nextInt(GARBAGE_TITLES.length)];
            case STREETLIGHT -> STREETLIGHT_TITLES[rand.nextInt(STREETLIGHT_TITLES.length)];
            case WATER_LEAK -> WATER_LEAK_TITLES[rand.nextInt(WATER_LEAK_TITLES.length)];
            case NOISE -> NOISE_TITLES[rand.nextInt(NOISE_TITLES.length)];
        };
    }

    private ComplaintStatus pickStatus(Random rand) {
        double r = rand.nextDouble();
        if (r < 0.25) return ComplaintStatus.SUBMITTED;
        if (r < 0.55) return ComplaintStatus.ASSIGNED;
        if (r < 0.80) return ComplaintStatus.IN_PROGRESS;
        return ComplaintStatus.RESOLVED;
    }

    private Map<IssueCategory, Department> getOrCreateDepartments() {
        Department roads = getOrSaveDept("Roads & Infrastructure", "ROADS_INFRA", "roads@civicheat.local");
        Department sanitation = getOrSaveDept("Sanitation", "SANITATION", "sanitation@civicheat.local");
        Department electricity = getOrSaveDept("Electricity", "ELECTRICITY", "electricity@civicheat.local");
        Department water = getOrSaveDept("Water Supply", "WATER_SUPPLY", "water@civicheat.local");
        Department pollution = getOrSaveDept("Pollution Control", "POLLUTION_CONTROL", "pollution@civicheat.local");

        return Map.of(
                IssueCategory.POTHOLE, roads,
                IssueCategory.GARBAGE, sanitation,
                IssueCategory.STREETLIGHT, electricity,
                IssueCategory.WATER_LEAK, water,
                IssueCategory.NOISE, pollution
        );
    }

    private Department getOrSaveDept(String name, String code, String email) {
        return departmentRepository.findByName(name)
                .orElseGet(() -> departmentRepository.save(Department.builder()
                        .name(name)
                        .code(code)
                        .contactEmail(email)
                        .build()));
    }
}
