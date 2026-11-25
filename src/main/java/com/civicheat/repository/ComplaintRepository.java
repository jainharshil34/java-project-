package com.civicheat.repository;

import com.civicheat.entity.Complaint;
import com.civicheat.enums.ComplaintStatus;
import com.civicheat.enums.IssueCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing Complaint entity persistence and PostGIS spatial queries.
 */
@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, UUID>, JpaSpecificationExecutor<Complaint> {

    @Override
    @EntityGraph(attributePaths = {"reporter", "department"})
    Page<Complaint> findAll(org.springframework.data.jpa.domain.Specification<Complaint> spec, Pageable pageable);

    /**
     * Finds nearby complaints matching the same category within a given radius in meters and time window,
     * utilizing PostGIS ST_DWithin and spatial indexing.
     *
     * @param lat          Latitude of the reference point
     * @param lng          Longitude of the reference point
     * @param radiusMeters Search radius in meters
     * @param category     Civic issue category name
     * @param sinceDate    Earliest submission timestamp to consider
     * @return List of neighboring complaints ordered by proximity
     */
    @Query(value = """
        SELECT c.* FROM complaints c
        WHERE c.category = :category
          AND c.created_at >= :sinceDate
          AND c.status <> 'DUPLICATE'
          AND ST_DWithin(
                c.location::geography,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                :radiusMeters
              )
        ORDER BY ST_Distance(
                c.location::geography,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
              ) ASC
        """, nativeQuery = true)
    List<Complaint> findNearbyByCategory(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters,
            @Param("category") String category,
            @Param("sinceDate") Instant sinceDate
    );

    /**
     * Finds non-resolved, non-rejected, non-duplicate complaints of the same category
     * within radius and time window for duplicate detection.
     */
    @Query(value = """
        SELECT c.* FROM complaints c
        WHERE c.category = :category
          AND c.created_at >= :sinceDate
          AND c.status NOT IN ('RESOLVED', 'REJECTED', 'DUPLICATE')
          AND ST_DWithin(
                c.location::geography,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                :radiusMeters
              )
        ORDER BY ST_Distance(
                c.location::geography,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
              ) ASC
        """, nativeQuery = true)
    List<Complaint> findDuplicateCandidates(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters,
            @Param("category") String category,
            @Param("sinceDate") Instant sinceDate
    );

    @Query(value = """
        SELECT count(c.id) FROM complaints c
        WHERE c.status NOT IN ('RESOLVED', 'REJECTED', 'DUPLICATE')
          AND (:excludeId IS NULL OR c.id <> :excludeId)
          AND ST_DWithin(
                c.location::geography,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                :radiusMeters
              )
        """, nativeQuery = true)
    long countOpenComplaintsNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters,
            @Param("excludeId") UUID excludeId
    );

    List<Complaint> findByStatusNotIn(java.util.Collection<ComplaintStatus> excludedStatuses);

    List<Complaint> findByStatusIn(java.util.Collection<ComplaintStatus> statuses);

    Page<Complaint> findByReporterIdOrderByCreatedAtDesc(UUID reporterId, Pageable pageable);

    List<Complaint> findByCategoryAndCreatedAtAfterAndStatusNotIn(
            IssueCategory category,
            Instant sinceDate,
            java.util.Collection<ComplaintStatus> excludedStatuses
    );

    Page<Complaint> findByDepartmentIdAndStatus(UUID departmentId, ComplaintStatus status, Pageable pageable);

    Page<Complaint> findByDepartmentId(UUID departmentId, Pageable pageable);

    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);

    Page<Complaint> findByCategory(IssueCategory category, Pageable pageable);

    List<Complaint> findByDuplicateOfId(UUID originalComplaintId);

    long countByDepartmentId(UUID departmentId);

    long countByDepartmentIdAndStatus(UUID departmentId, ComplaintStatus status);

    long countByCategory(IssueCategory category);

    long countByStatus(ComplaintStatus status);
}
