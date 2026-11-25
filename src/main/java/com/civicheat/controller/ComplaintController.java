package com.civicheat.controller;

import com.civicheat.dto.request.ComplaintCreateRequest;
import com.civicheat.dto.response.ComplaintDetailResponse;
import com.civicheat.dto.response.ComplaintResponse;
import com.civicheat.dto.response.PageResponse;
import com.civicheat.enums.ComplaintStatus;
import com.civicheat.enums.IssueCategory;
import com.civicheat.enums.SeverityLevel;
import com.civicheat.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller exposing citizen and public APIs for civic issue complaint lifecycle management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/complaints")
@RequiredArgsConstructor
@Tag(name = "Complaints", description = "Endpoints for reporting, querying, and upvoting civic complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final com.civicheat.security.RateLimiter rateLimiter;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Submit a new civic complaint",
            description = "Submits a geotagged complaint with JSON details and up to 4 optional image attachments"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Complaint submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or file format"),
            @ApiResponse(responseCode = "413", description = "Payload or file size exceeded"),
            @ApiResponse(responseCode = "429", description = "Too many requests - rate limit exceeded")
    })
    public ResponseEntity<ComplaintDetailResponse> createComplaint(
            @Parameter(description = "Complaint JSON data", required = true, schema = @Schema(type = "string", format = "binary"))
            @RequestPart("complaint") @Valid ComplaintCreateRequest request,

            @Parameter(description = "Optional image attachments (up to 4 files, JPEG/PNG/WebP)")
            @RequestPart(value = "images", required = false) List<MultipartFile> images,

            Principal principal,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        String clientKey = principal != null ? principal.getName() : httpRequest.getRemoteAddr();
        rateLimiter.checkLimit(clientKey);

        String userEmail = principal != null ? principal.getName() : "citizen@civicheat.local";
        ComplaintDetailResponse response = complaintService.createComplaint(request, images, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get complaint by ID", description = "Retrieves full complaint details including status history and attached media")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Complaint found"),
            @ApiResponse(responseCode = "404", description = "Complaint not found")
    })
    public ResponseEntity<ComplaintDetailResponse> getComplaintById(
            @Parameter(description = "UUID of the complaint", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(complaintService.getComplaintById(id));
    }

    @GetMapping
    @Operation(
            summary = "Query complaints with filters",
            description = "Retrieves paginated complaints filtered by category, status, severity, spatial bounding box, and dates"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of complaints")
    })
    public ResponseEntity<PageResponse<ComplaintResponse>> getComplaints(
            @Parameter(description = "Filter by issue category")
            @RequestParam(required = false) IssueCategory category,

            @Parameter(description = "Filter by complaint status")
            @RequestParam(required = false) ComplaintStatus status,

            @Parameter(description = "Filter by severity level")
            @RequestParam(required = false) SeverityLevel severity,

            @Parameter(description = "Bounding box minimum latitude")
            @RequestParam(required = false) Double minLat,

            @Parameter(description = "Bounding box maximum latitude")
            @RequestParam(required = false) Double maxLat,

            @Parameter(description = "Bounding box minimum longitude")
            @RequestParam(required = false) Double minLon,

            @Parameter(description = "Bounding box maximum longitude")
            @RequestParam(required = false) Double maxLon,

            @Parameter(description = "Earliest creation date (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,

            @Parameter(description = "Latest creation date (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,

            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort property", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction (asc/desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<ComplaintResponse> response = complaintService.getComplaints(
                category, status, severity,
                minLat, maxLat, minLon, maxLon,
                startDate, endDate, pageable
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/upvote")
    @Operation(
            summary = "Upvote a civic complaint",
            description = "Increments upvote counter for an issue. Enforces one upvote per user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Complaint successfully upvoted"),
            @ApiResponse(responseCode = "400", description = "User has already upvoted this complaint"),
            @ApiResponse(responseCode = "404", description = "Complaint not found")
    })
    public ResponseEntity<ComplaintResponse> upvoteComplaint(
            @Parameter(description = "UUID of the complaint to upvote", required = true)
            @PathVariable UUID id,
            Principal principal) {

        String userEmail = principal != null ? principal.getName() : "citizen@civicheat.local";
        return ResponseEntity.ok(complaintService.upvoteComplaint(id, userEmail));
    }

    @GetMapping("/mine")
    @Operation(
            summary = "Get authenticated citizen's submitted complaints",
            description = "Retrieves all complaints reported by the currently authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of citizen's complaints")
    })
    public ResponseEntity<PageResponse<ComplaintResponse>> getMyComplaints(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,

            Principal principal) {

        String userEmail = principal != null ? principal.getName() : "citizen@civicheat.local";
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(complaintService.getMyComplaints(userEmail, pageable));
    }
}
