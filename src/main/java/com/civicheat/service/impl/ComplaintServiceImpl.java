package com.civicheat.service.impl;

import com.civicheat.dto.request.ComplaintCreateRequest;
import com.civicheat.dto.response.ComplaintDetailResponse;
import com.civicheat.dto.response.ComplaintResponse;
import com.civicheat.dto.response.PageResponse;
import com.civicheat.entity.Complaint;
import com.civicheat.entity.ComplaintImage;
import com.civicheat.entity.ComplaintStatusHistory;
import com.civicheat.entity.ComplaintUpvote;
import com.civicheat.entity.ComplaintUpvoteId;
import com.civicheat.entity.Department;
import com.civicheat.entity.User;
import com.civicheat.enums.ComplaintStatus;
import com.civicheat.enums.IssueCategory;
import com.civicheat.enums.Role;
import com.civicheat.enums.SeverityLevel;
import com.civicheat.exception.InvalidFileException;
import com.civicheat.exception.ResourceNotFoundException;
import com.civicheat.mapper.ComplaintMapper;
import com.civicheat.repository.ComplaintImageRepository;
import com.civicheat.repository.ComplaintRepository;
import com.civicheat.repository.ComplaintSpecification;
import com.civicheat.repository.ComplaintStatusHistoryRepository;
import com.civicheat.repository.ComplaintUpvoteRepository;
import com.civicheat.repository.UserRepository;
import com.civicheat.service.ComplaintService;
import com.civicheat.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Production implementation of ComplaintService coordinating persistence, media upload, and engine hooks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplaintService {

    public static final int MAX_IMAGES_PER_COMPLAINT = 4;

    private final ComplaintRepository complaintRepository;
    private final ComplaintImageRepository complaintImageRepository;
    private final ComplaintStatusHistoryRepository statusHistoryRepository;
    private final ComplaintUpvoteRepository upvoteRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ComplaintMapper complaintMapper;
    private final com.civicheat.engine.duplicate.DuplicateDetector duplicateDetector;
    private final com.civicheat.engine.severity.SeverityCalculator severityCalculator;
    private final com.civicheat.engine.routing.RoutingStrategy routingStrategy;

    @Override
    @Transactional
    public ComplaintDetailResponse createComplaint(
            ComplaintCreateRequest request, List<MultipartFile> images, String userEmail) {
        log.info("Processing new complaint submission: title='{}', category={}", request.getTitle(), request.getCategory());

        if (images != null && images.size() > MAX_IMAGES_PER_COMPLAINT) {
            throw new InvalidFileException("A maximum of 4 image attachments is permitted per complaint");
        }

        User reporter = resolveUser(userEmail);
        Complaint complaint = complaintMapper.toEntity(request, reporter);

        // =========================================================================
        // [HOOK POINT 1: DUPLICATE DETECTION STRATEGY]
        // =========================================================================
        Optional<Complaint> duplicateOpt = duplicateDetector.findDuplicate(request);
        boolean isDuplicate = duplicateOpt.isPresent();
        Complaint originalComplaint = null;

        if (isDuplicate) {
            originalComplaint = duplicateOpt.get();
            complaint.setStatus(ComplaintStatus.DUPLICATE);
            complaint.setDuplicateOf(originalComplaint);
            complaint.setDepartment(originalComplaint.getDepartment());
            complaint.setSeverityScore(originalComplaint.getSeverityScore());
            complaint.setSeverityLevel(originalComplaint.getSeverityLevel());

            int updatedUpvotes = (originalComplaint.getUpvoteCount() != null ? originalComplaint.getUpvoteCount() : 0) + 1;
            originalComplaint.setUpvoteCount(updatedUpvotes);
            complaintRepository.save(originalComplaint);

            log.info("Linked new report as duplicate of original complaint ID: {}. Incremented original upvote count to {}",
                    originalComplaint.getId(), updatedUpvotes);
        }

        // =========================================================================
        // [HOOK POINT 2: SEVERITY CALCULATION STRATEGY]
        // =========================================================================
        com.civicheat.engine.severity.SeverityResult severityResult = null;
        if (!isDuplicate) {
            severityResult = severityCalculator.calculate(complaint);
            complaint.setSeverityScore(severityResult.getScore());
            complaint.setSeverityLevel(severityResult.getLevel());
            log.debug("Computed initial severity score: {} ({})", severityResult.getScore(), severityResult.getLevel());
        }

        // =========================================================================
        // [HOOK POINT 3: DEPARTMENT ROUTING STRATEGY]
        // =========================================================================
        Department routedDepartment = null;
        if (!isDuplicate) {
            routedDepartment = routingStrategy.route(complaint);
            complaint.setDepartment(routedDepartment);
            complaint.setStatus(ComplaintStatus.ASSIGNED);
            log.info("Auto-routed complaint to department: {}", routedDepartment != null ? routedDepartment.getName() : "None");
        }

        // Save Complaint entity
        Complaint savedComplaint = complaintRepository.save(complaint);

        // Process and store image attachments if present
        if (images != null && !images.isEmpty()) {
            List<ComplaintImage> complaintImages = new ArrayList<>();
            for (MultipartFile file : images) {
                if (file != null && !file.isEmpty()) {
                    String storedFilename = fileStorageService.storeFile(file);
                    String fileUrl = "/api/v1/files/" + storedFilename;

                    ComplaintImage imageEntity = ComplaintImage.builder()
                            .complaint(savedComplaint)
                            .imageUrl(fileUrl)
                            .fileName(storedFilename)
                            .contentType(file.getContentType())
                            .fileSize(file.getSize())
                            .build();

                    complaintImages.add(imageEntity);
                }
            }
            if (!complaintImages.isEmpty()) {
                complaintImageRepository.saveAll(complaintImages);
                savedComplaint.setImages(complaintImages);
            }
        }

        // Record initial status history audit trail
        String remarks;
        if (isDuplicate) {
            remarks = "Complaint automatically identified as duplicate of #" + originalComplaint.getId();
        } else if (routedDepartment != null) {
            remarks = "Complaint submitted by citizen and auto-routed to " + routedDepartment.getName();
        } else {
            remarks = "Complaint submitted by citizen";
        }

        ComplaintStatusHistory initialHistory = ComplaintStatusHistory.builder()
                .complaint(savedComplaint)
                .previousStatus(null)
                .newStatus(savedComplaint.getStatus())
                .changedBy(reporter)
                .remarks(remarks)
                .build();

        statusHistoryRepository.save(initialHistory);
        savedComplaint.setStatusHistories(new ArrayList<>(List.of(initialHistory)));

        log.info("Successfully created complaint ID: {} with status: {}", savedComplaint.getId(), savedComplaint.getStatus());
        ComplaintDetailResponse response = complaintMapper.toDetailResponse(savedComplaint);
        if (severityResult != null) {
            response.setSeverityBreakdown(severityResult.getBreakdown());
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintDetailResponse getComplaintById(UUID id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + id));
        ComplaintDetailResponse response = complaintMapper.toDetailResponse(complaint);
        com.civicheat.engine.severity.SeverityResult severityResult = severityCalculator.calculate(complaint);
        response.setSeverityBreakdown(severityResult.getBreakdown());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ComplaintResponse> getComplaints(
            IssueCategory category,
            ComplaintStatus status,
            SeverityLevel severityLevel,
            Double minLat,
            Double maxLat,
            Double minLon,
            Double maxLon,
            Instant startDate,
            Instant endDate,
            Pageable pageable) {

        Specification<Complaint> spec = ComplaintSpecification.withFilters(
                category, status, severityLevel,
                minLat, maxLat, minLon, maxLon,
                startDate, endDate, null, null
        );

        Page<Complaint> page = complaintRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(complaintMapper::toResponse));
    }

    @Override
    @Transactional
    public ComplaintResponse upvoteComplaint(UUID id, String userEmail) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + id));

        User user = resolveUser(userEmail);

        ComplaintUpvoteId upvoteId = new ComplaintUpvoteId(complaint.getId(), user.getId());
        if (upvoteRepository.existsById(upvoteId)) {
            throw new IllegalArgumentException("User has already upvoted this complaint");
        }

        ComplaintUpvote upvote = ComplaintUpvote.builder()
                .id(upvoteId)
                .complaint(complaint)
                .user(user)
                .build();

        upvoteRepository.save(upvote);

        int newCount = (complaint.getUpvoteCount() != null ? complaint.getUpvoteCount() : 0) + 1;
        complaint.setUpvoteCount(newCount);
        Complaint updated = complaintRepository.save(complaint);

        log.info("Complaint {} upvoted by user {}. Total upvotes: {}", id, userEmail, newCount);
        return complaintMapper.toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ComplaintResponse> getMyComplaints(String userEmail, Pageable pageable) {
        User user = resolveUser(userEmail);
        Page<Complaint> page = complaintRepository.findByReporterIdOrderByCreatedAtDesc(user.getId(), pageable);
        return PageResponse.from(page.map(complaintMapper::toResponse));
    }

    /**
     * Resolves the current user by email, or creates a default citizen user if not yet initialized.
     */
    private User resolveUser(String email) {
        String effectiveEmail = (email != null && !email.isBlank()) ? email : "anonymous@civicheat.local";
        return userRepository.findByEmail(effectiveEmail)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(effectiveEmail)
                                .password("{noop}password")
                                .fullName(effectiveEmail.split("@")[0])
                                .role(Role.CITIZEN)
                                .build()
                ));
    }
}
