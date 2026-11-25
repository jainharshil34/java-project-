package com.civicheat.repository;

import com.civicheat.entity.ComplaintImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing ComplaintImage entity persistence.
 */
@Repository
public interface ComplaintImageRepository extends JpaRepository<ComplaintImage, UUID> {

    List<ComplaintImage> findByComplaintIdOrderByCreatedAtAsc(UUID complaintId);

    long countByComplaintId(UUID complaintId);
}
