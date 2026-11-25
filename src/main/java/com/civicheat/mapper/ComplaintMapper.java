package com.civicheat.mapper;

import com.civicheat.dto.request.ComplaintCreateRequest;
import com.civicheat.dto.response.ComplaintDetailResponse;
import com.civicheat.dto.response.ComplaintImageResponse;
import com.civicheat.dto.response.ComplaintResponse;
import com.civicheat.dto.response.ComplaintStatusHistoryResponse;
import com.civicheat.entity.Complaint;
import com.civicheat.entity.ComplaintImage;
import com.civicheat.entity.ComplaintStatusHistory;
import com.civicheat.entity.User;
import com.civicheat.enums.ComplaintStatus;
import com.civicheat.enums.SeverityLevel;
import com.civicheat.util.GeoUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper component translating between Complaint domain entities and API DTOs.
 */
@Component
public class ComplaintMapper {

    /**
     * Converts a ComplaintCreateRequest DTO into an unpersisted Complaint entity.
     *
     * @param request  Incoming creation request
     * @param reporter User filing the report
     * @return Initialized Complaint entity
     */
    public Complaint toEntity(ComplaintCreateRequest request, User reporter) {
        if (request == null) {
            return null;
        }

        return Complaint.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .location(GeoUtils.createPoint(request.getLatitude(), request.getLongitude()))
                .address(request.getAddress())
                .severityScore(0)
                .severityLevel(SeverityLevel.LOW)
                .status(ComplaintStatus.SUBMITTED)
                .upvoteCount(0)
                .reporter(reporter)
                .build();
    }

    /**
     * Converts a Complaint entity into a lightweight ComplaintResponse DTO.
     *
     * @param complaint Complaint entity
     * @return ComplaintResponse DTO
     */
    public ComplaintResponse toResponse(Complaint complaint) {
        if (complaint == null) {
            return null;
        }

        List<String> imageUrls = complaint.getImages() != null
                ? complaint.getImages().stream().map(ComplaintImage::getImageUrl).toList()
                : Collections.emptyList();

        return ComplaintResponse.builder()
                .id(complaint.getId())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(complaint.getCategory())
                .latitude(complaint.getLatitude())
                .longitude(complaint.getLongitude())
                .address(complaint.getAddress())
                .severityScore(complaint.getSeverityScore())
                .severityLevel(complaint.getSeverityLevel())
                .status(complaint.getStatus())
                .upvoteCount(complaint.getUpvoteCount())
                .duplicateOfId(complaint.getDuplicateOf() != null ? complaint.getDuplicateOf().getId() : null)
                .duplicate(complaint.getStatus() == ComplaintStatus.DUPLICATE || complaint.getDuplicateOf() != null)
                .reporterId(complaint.getReporter() != null ? complaint.getReporter().getId() : null)
                .reporterName(complaint.getReporter() != null ? complaint.getReporter().getFullName() : null)
                .departmentId(complaint.getDepartment() != null ? complaint.getDepartment().getId() : null)
                .departmentName(complaint.getDepartment() != null ? complaint.getDepartment().getName() : null)
                .imageUrls(imageUrls)
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .resolvedAt(complaint.getResolvedAt())
                .build();
    }

    /**
     * Converts a Complaint entity into a comprehensive ComplaintDetailResponse DTO.
     *
     * @param complaint Complaint entity
     * @return Detailed response with media and status histories
     */
    public ComplaintDetailResponse toDetailResponse(Complaint complaint) {
        if (complaint == null) {
            return null;
        }

        List<ComplaintImageResponse> imageResponses = complaint.getImages() != null
                ? complaint.getImages().stream().map(this::toImageResponse).toList()
                : Collections.emptyList();

        List<ComplaintStatusHistoryResponse> historyResponses = complaint.getStatusHistories() != null
                ? complaint.getStatusHistories().stream().map(this::toStatusHistoryResponse).toList()
                : Collections.emptyList();

        return ComplaintDetailResponse.builder()
                .id(complaint.getId())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(complaint.getCategory())
                .latitude(complaint.getLatitude())
                .longitude(complaint.getLongitude())
                .address(complaint.getAddress())
                .severityScore(complaint.getSeverityScore())
                .severityLevel(complaint.getSeverityLevel())
                .status(complaint.getStatus())
                .upvoteCount(complaint.getUpvoteCount())
                .duplicateOfId(complaint.getDuplicateOf() != null ? complaint.getDuplicateOf().getId() : null)
                .duplicate(complaint.getStatus() == ComplaintStatus.DUPLICATE || complaint.getDuplicateOf() != null)
                .reporterId(complaint.getReporter() != null ? complaint.getReporter().getId() : null)
                .reporterName(complaint.getReporter() != null ? complaint.getReporter().getFullName() : null)
                .reporterEmail(complaint.getReporter() != null ? complaint.getReporter().getEmail() : null)
                .departmentId(complaint.getDepartment() != null ? complaint.getDepartment().getId() : null)
                .departmentName(complaint.getDepartment() != null ? complaint.getDepartment().getName() : null)
                .images(imageResponses)
                .statusHistories(historyResponses)
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .resolvedAt(complaint.getResolvedAt())
                .build();
    }

    /**
     * Maps a ComplaintImage entity to ComplaintImageResponse DTO.
     */
    public ComplaintImageResponse toImageResponse(ComplaintImage image) {
        if (image == null) {
            return null;
        }
        return ComplaintImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .fileName(image.getFileName())
                .contentType(image.getContentType())
                .fileSize(image.getFileSize())
                .createdAt(image.getCreatedAt())
                .build();
    }

    /**
     * Maps a ComplaintStatusHistory entity to ComplaintStatusHistoryResponse DTO.
     */
    public ComplaintStatusHistoryResponse toStatusHistoryResponse(ComplaintStatusHistory history) {
        if (history == null) {
            return null;
        }
        return ComplaintStatusHistoryResponse.builder()
                .id(history.getId())
                .previousStatus(history.getPreviousStatus())
                .newStatus(history.getNewStatus())
                .changedByUserId(history.getChangedBy() != null ? history.getChangedBy().getId() : null)
                .changedByUserName(history.getChangedBy() != null ? history.getChangedBy().getFullName() : null)
                .remarks(history.getRemarks())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
