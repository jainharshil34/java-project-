package com.civicheat.service.impl;

import com.civicheat.dto.request.CreateOfficerRequest;
import com.civicheat.dto.request.ReassignComplaintRequest;
import com.civicheat.dto.request.UpdateComplaintStatusRequest;
import com.civicheat.dto.response.ComplaintDetailResponse;
import com.civicheat.dto.response.UserResponse;
import com.civicheat.entity.Complaint;
import com.civicheat.entity.ComplaintStatusHistory;
import com.civicheat.entity.Department;
import com.civicheat.entity.User;
import com.civicheat.enums.ComplaintStatus;
import com.civicheat.enums.Role;
import com.civicheat.exception.DuplicateResourceException;
import com.civicheat.exception.ResourceNotFoundException;
import com.civicheat.mapper.ComplaintMapper;
import com.civicheat.mapper.UserMapper;
import com.civicheat.repository.ComplaintRepository;
import com.civicheat.repository.ComplaintStatusHistoryRepository;
import com.civicheat.repository.DepartmentRepository;
import com.civicheat.repository.UserRepository;
import com.civicheat.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of AdminService managing officer creation, complaint reassignment, and status transition workflows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintStatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final ComplaintMapper complaintMapper;
    private final UserMapper userMapper;

    /**
     * Finite state machine defining permitted lifecycle status transitions.
     */
    private static final Map<ComplaintStatus, Set<ComplaintStatus>> ALLOWED_TRANSITIONS = Map.of(
            ComplaintStatus.SUBMITTED, EnumSet.of(ComplaintStatus.VERIFIED, ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS, ComplaintStatus.REJECTED, ComplaintStatus.DUPLICATE),
            ComplaintStatus.VERIFIED, EnumSet.of(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS, ComplaintStatus.REJECTED),
            ComplaintStatus.ASSIGNED, EnumSet.of(ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED, ComplaintStatus.REJECTED),
            ComplaintStatus.IN_PROGRESS, EnumSet.of(ComplaintStatus.RESOLVED, ComplaintStatus.REJECTED, ComplaintStatus.ASSIGNED),
            ComplaintStatus.RESOLVED, EnumSet.of(ComplaintStatus.IN_PROGRESS), // Reopen
            ComplaintStatus.REJECTED, EnumSet.of(ComplaintStatus.IN_PROGRESS, ComplaintStatus.ASSIGNED),
            ComplaintStatus.DUPLICATE, EnumSet.of(ComplaintStatus.ASSIGNED, ComplaintStatus.REJECTED)
    );

    @Override
    @Transactional
    public ComplaintDetailResponse updateComplaintStatus(
            UUID complaintId, UpdateComplaintStatusRequest request, String currentUserEmail) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + complaintId));

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + currentUserEmail));

        // 1. Role-based Departmental Authorization
        if (currentUser.getRole() == Role.OFFICER) {
            if (currentUser.getDepartment() == null || complaint.getDepartment() == null ||
                    !currentUser.getDepartment().getId().equals(complaint.getDepartment().getId())) {
                log.warn("Officer {} denied permission to update complaint {} belonging to department {}",
                        currentUserEmail, complaintId, complaint.getDepartment() != null ? complaint.getDepartment().getName() : "None");
                throw new AccessDeniedException("Officers can only update complaints within their own department");
            }
        }

        ComplaintStatus currentStatus = complaint.getStatus();
        ComplaintStatus targetStatus = request.getStatus();

        // 2. Validate Allowed Lifecycle Transition
        if (currentStatus != targetStatus) {
            Set<ComplaintStatus> allowedTargets = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(ComplaintStatus.class));
            if (!allowedTargets.contains(targetStatus)) {
                throw new IllegalArgumentException(
                        String.format("Invalid status transition from %s to %s", currentStatus, targetStatus));
            }
        }

        // 3. Update Status and Resolution Timestamp
        complaint.setStatus(targetStatus);
        if (targetStatus == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(Instant.now());
        } else if (currentStatus == ComplaintStatus.RESOLVED && targetStatus != ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(null);
        }

        Complaint savedComplaint = complaintRepository.save(complaint);

        // 4. Record Audit Trail History
        String remarks = (request.getRemark() != null && !request.getRemark().isBlank())
                ? request.getRemark().trim()
                : "Status updated from " + currentStatus + " to " + targetStatus + " by " + currentUser.getFullName();

        ComplaintStatusHistory history = ComplaintStatusHistory.builder()
                .complaint(savedComplaint)
                .previousStatus(currentStatus)
                .newStatus(targetStatus)
                .changedBy(currentUser)
                .remarks(remarks)
                .build();

        statusHistoryRepository.save(history);
        savedComplaint.getStatusHistories().add(history);

        log.info("Complaint {} status transitioned from {} to {} by {}", complaintId, currentStatus, targetStatus, currentUserEmail);
        return complaintMapper.toDetailResponse(savedComplaint);
    }

    @Override
    @Transactional
    public UserResponse createOfficer(CreateOfficerRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("Creating new departmental officer account for email: {}", email);

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User already exists with email: " + email);
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        User officer = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .role(Role.OFFICER)
                .department(department)
                .build();

        User savedOfficer = userRepository.save(officer);
        log.info("Successfully provisioned officer ID: {} assigned to department: {}", savedOfficer.getId(), department.getName());
        return userMapper.toResponse(savedOfficer);
    }

    @Override
    @Transactional
    public ComplaintDetailResponse reassignComplaint(
            UUID complaintId, ReassignComplaintRequest request, String currentUserEmail) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + complaintId));

        Department targetDepartment = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        User adminUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with email: " + currentUserEmail));

        Department previousDepartment = complaint.getDepartment();
        complaint.setDepartment(targetDepartment);

        if (complaint.getStatus() == ComplaintStatus.SUBMITTED || complaint.getStatus() == ComplaintStatus.VERIFIED) {
            complaint.setStatus(ComplaintStatus.ASSIGNED);
        }

        Complaint savedComplaint = complaintRepository.save(complaint);

        String reason = (request.getReason() != null && !request.getReason().isBlank()) ? ": " + request.getReason().trim() : "";
        String remarks = String.format("Reassigned from %s to %s%s",
                previousDepartment != null ? previousDepartment.getName() : "None",
                targetDepartment.getName(),
                reason);

        ComplaintStatusHistory history = ComplaintStatusHistory.builder()
                .complaint(savedComplaint)
                .previousStatus(complaint.getStatus())
                .newStatus(savedComplaint.getStatus())
                .changedBy(adminUser)
                .remarks(remarks)
                .build();

        statusHistoryRepository.save(history);
        savedComplaint.getStatusHistories().add(history);

        log.info("Complaint {} reassigned to department {} by admin {}",
                complaintId, targetDepartment.getName(), currentUserEmail);

        return complaintMapper.toDetailResponse(savedComplaint);
    }
}
