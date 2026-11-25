package com.civicheat.controller;

import com.civicheat.entity.Complaint;
import com.civicheat.enums.ComplaintStatus;
import com.civicheat.enums.IssueCategory;
import com.civicheat.repository.ComplaintRepository;
import com.civicheat.repository.ComplaintSpecification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Data export endpoint: download civic complaints as a CSV file for offline analysis.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
@Tag(name = "Export", description = "Download complaints data in CSV format")
public class ExportController {

    private final ComplaintRepository complaintRepository;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    @GetMapping("/complaints.csv")
    @Operation(summary = "Export filtered complaints as CSV")
    public void exportCsv(
            @RequestParam(required = false) IssueCategory category,
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=civicheat-complaints.csv");

        Specification<Complaint> spec = ComplaintSpecification.withFilters(
                category, status, null,
                null, null, null, null,
                startDate, endDate, null, null);

        List<Complaint> complaints = complaintRepository.findAll(spec);
        log.info("Exporting {} complaints to CSV", complaints.size());

        try (PrintWriter writer = response.getWriter()) {
            writer.println("ID,Title,Category,Status,SeverityScore,SeverityLevel," +
                          "Latitude,Longitude,Address,Department,ReporterEmail," +
                          "UpvoteCount,CreatedAt,ResolvedAt");

            for (Complaint c : complaints) {
                writer.printf("%s,%s,%s,%s,%d,%s,%.6f,%.6f,%s,%s,%s,%d,%s,%s%n",
                    c.getId(),
                    escapeCsv(c.getTitle()),
                    c.getCategory(),
                    c.getStatus(),
                    c.getSeverityScore(),
                    c.getSeverityLevel(),
                    c.getLatitude(),
                    c.getLongitude(),
                    escapeCsv(c.getAddress() != null ? c.getAddress() : ""),
                    escapeCsv(c.getDepartment() != null ? c.getDepartment().getName() : ""),
                    escapeCsv(c.getReporter() != null ? c.getReporter().getEmail() : ""),
                    c.getUpvoteCount(),
                    FMT.format(c.getCreatedAt()),
                    c.getResolvedAt() != null ? FMT.format(c.getResolvedAt()) : ""
                );
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
