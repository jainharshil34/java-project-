package com.civicheat.controller;

import com.civicheat.dto.request.CreateOfficerRequest;
import com.civicheat.dto.request.ReassignComplaintRequest;
import com.civicheat.dto.request.UpdateComplaintStatusRequest;
import com.civicheat.dto.response.ComplaintDetailResponse;
import com.civicheat.dto.response.UserResponse;
import com.civicheat.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for municipal administration, officer creation, complaint reassignment, and status transitions.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administration & Workflows", description = "Endpoints for status updates, department reassignment, and officer provisioning")
public class AdminController {

    private final AdminService adminService;

    @PatchMapping("/complaints/{id}/status")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    @Operation(summary = "Update complaint status", description = "Transitions complaint status following lifecycle rules. Officers may only update complaints within their department; Admins may update any.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Complaint status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition or invalid payload"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Officer not authorized for this department"),
            @ApiResponse(responseCode = "404", description = "Complaint not found")
    })
    public ResponseEntity<ComplaintDetailResponse> updateStatus(
            @Parameter(description = "Complaint UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody UpdateComplaintStatusRequest request,
            Authentication authentication) {

        String currentUserEmail = authentication.getName();
        ComplaintDetailResponse response = adminService.updateComplaintStatus(id, request, currentUserEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/officers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create department officer", description = "Provisions a new departmental officer account linked to a specific municipal department. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Officer account created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error in request payload"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin privilege required"),
            @ApiResponse(responseCode = "404", description = "Target department not found"),
            @ApiResponse(responseCode = "409", description = "User with this email already exists")
    })
    public ResponseEntity<UserResponse> createOfficer(
            @Valid @RequestBody CreateOfficerRequest request) {

        UserResponse response = adminService.createOfficer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/complaints/{id}/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reassign complaint department", description = "Reassigns a complaint to a different municipal department. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Complaint successfully reassigned"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin privilege required"),
            @ApiResponse(responseCode = "404", description = "Complaint or Department not found")
    })
    public ResponseEntity<ComplaintDetailResponse> reassignComplaint(
            @Parameter(description = "Complaint UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody ReassignComplaintRequest request,
            Authentication authentication) {

        String currentUserEmail = authentication.getName();
        ComplaintDetailResponse response = adminService.reassignComplaint(id, request, currentUserEmail);
        return ResponseEntity.ok(response);
    }
}
