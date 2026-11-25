package com.civicheat.repository;

import com.civicheat.entity.ComplaintUpvote;
import com.civicheat.entity.ComplaintUpvoteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for managing ComplaintUpvote persistence.
 */
@Repository
public interface ComplaintUpvoteRepository extends JpaRepository<ComplaintUpvote, ComplaintUpvoteId> {

    boolean existsById(ComplaintUpvoteId id);

    boolean existsByComplaintIdAndUserId(UUID complaintId, UUID userId);

    long countByComplaintId(UUID complaintId);
}
