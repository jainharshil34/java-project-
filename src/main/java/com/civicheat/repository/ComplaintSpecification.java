package com.civicheat.repository;

import com.civicheat.entity.Complaint;
import com.civicheat.enums.ComplaintStatus;
import com.civicheat.enums.IssueCategory;
import com.civicheat.enums.SeverityLevel;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dynamic JPA Specification builder for querying complaints with multi-criteria filters.
 */
public final class ComplaintSpecification {

    private ComplaintSpecification() {
        // Utility class
    }

    /**
     * Builds a composite Specification based on provided filter criteria.
     */
    public static Specification<Complaint> withFilters(
            IssueCategory category,
            ComplaintStatus status,
            SeverityLevel severityLevel,
            Double minLat,
            Double maxLat,
            Double minLon,
            Double maxLon,
            Instant startDate,
            Instant endDate,
            UUID reporterId,
            UUID departmentId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (severityLevel != null) {
                predicates.add(cb.equal(root.get("severityLevel"), severityLevel));
            }

            if (reporterId != null) {
                predicates.add(cb.equal(root.get("reporter").get("id"), reporterId));
            }

            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }

            if (minLat != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("latitude"), minLat));
            }
            if (maxLat != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("latitude"), maxLat));
            }

            if (minLon != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("longitude"), minLon));
            }
            if (maxLon != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("longitude"), maxLon));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
