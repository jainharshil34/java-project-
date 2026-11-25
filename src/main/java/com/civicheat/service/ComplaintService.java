package com.civicheat.service;

import com.civicheat.dto.request.ComplaintCreateRequest;
import com.civicheat.dto.response.ComplaintDetailResponse;
import com.civicheat.dto.response.ComplaintResponse;
import com.civicheat.dto.response.PageResponse;
import com.civicheat.enums.ComplaintStatus;
import com.civicheat.enums.IssueCategory;
import com.civicheat.enums.SeverityLevel;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service interface governing civic complaint submission, retrieval, upvoting, and query lifecycle.
 */
public interface ComplaintService {

    /**
     * Creates and persists a new civic complaint along with optional image attachments.
     *
     * @param request   Complaint payload
     * @param images    Optional image files (maximum 4)
     * @param userEmail Email of the authenticated reporter
     * @return Detailed representation of the created complaint
     */
    ComplaintDetailResponse createComplaint(ComplaintCreateRequest request, List<MultipartFile> images, String userEmail);

    /**
     * Retrieves a complaint by its unique identifier.
     *
     * @param id Complaint unique ID
     * @return Detailed complaint representation
     */
    ComplaintDetailResponse getComplaintById(UUID id);

    /**
     * Queries complaints matching dynamic multi-criteria filters with pagination and sorting.
     */
    PageResponse<ComplaintResponse> getComplaints(
            IssueCategory category,
            ComplaintStatus status,
            SeverityLevel severityLevel,
            Double minLat,
            Double maxLat,
            Double minLon,
            Double maxLon,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    );

    /**
     * Registers a citizen upvote on a civic complaint. Enforces one upvote per user.
     *
     * @param id        Complaint ID to upvote
     * @param userEmail Email of the authenticated user casting the vote
     * @return Updated complaint response
     */
    ComplaintResponse upvoteComplaint(UUID id, String userEmail);

    /**
     * Retrieves all complaints submitted by the authenticated citizen.
     *
     * @param userEmail Email of the authenticated user
     * @param pageable  Pagination details
     * @return Paginated complaints for current user
     */
    PageResponse<ComplaintResponse> getMyComplaints(String userEmail, Pageable pageable);
}
