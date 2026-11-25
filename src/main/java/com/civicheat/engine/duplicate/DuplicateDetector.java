package com.civicheat.engine.duplicate;

import com.civicheat.dto.request.ComplaintCreateRequest;
import com.civicheat.entity.Complaint;

import java.util.Optional;

/**
 * Strategy contract for detecting duplicate civic complaint submissions.
 */
public interface DuplicateDetector {

    /**
     * Inspects active existing complaints to determine if the incoming complaint is a duplicate.
     *
     * @param req The incoming complaint creation request
     * @return Optional containing the matched original Complaint if duplicate, or empty
     */
    Optional<Complaint> findDuplicate(ComplaintCreateRequest req);
}
