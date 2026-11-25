package com.civicheat.service;

import com.civicheat.dto.request.CreateOfficerRequest;
import com.civicheat.dto.request.ReassignComplaintRequest;
import com.civicheat.dto.request.UpdateComplaintStatusRequest;
import com.civicheat.dto.response.ComplaintDetailResponse;
import com.civicheat.dto.response.UserResponse;

import java.util.UUID;

/**
 * Service interface for municipal administration, officer provisioning, and complaint workflow lifecycle management.
 */
public interface AdminService {

    /**
     * Updates complaint lifecycle status with role authorization and state transition validation.
     *
     * @param complaintId the target complaint ID
     * @param request the status update payload
     * @param currentUserEmail the authenticated officer or admin's email
     * @return updated complaint details
     */
    ComplaintDetailResponse updateComplaintStatus(UUID complaintId, UpdateComplaintStatusRequest request, String currentUserEmail);

    /**
     * Registers a new departmental officer user account.
     *
     * @param request the officer creation request
     * @return created officer profile response
     */
    UserResponse createOfficer(CreateOfficerRequest request);

    /**
     * Reassigns a complaint to a different municipal department.
     *
     * @param complaintId the complaint ID
     * @param request the reassignment request payload
     * @param currentUserEmail the authenticated admin's email
     * @return updated complaint details
     */
    ComplaintDetailResponse reassignComplaint(UUID complaintId, ReassignComplaintRequest request, String currentUserEmail);
}
