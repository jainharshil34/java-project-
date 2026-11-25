package com.civicheat.repository;

import com.civicheat.entity.ComplaintComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ComplaintCommentRepository extends JpaRepository<ComplaintComment, UUID> {
    Page<ComplaintComment> findByComplaintIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID complaintId, Pageable pageable);
    long countByComplaintIdAndIsDeletedFalse(UUID complaintId);
}
