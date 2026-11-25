package com.civicheat.controller;

import com.civicheat.dto.response.ComplaintResponse;
import com.civicheat.dto.response.DepartmentResponse;
import com.civicheat.dto.response.PageResponse;
import com.civicheat.enums.ComplaintStatus;
import com.civicheat.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller exposing municipal department discovery and department task queue endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Endpoints for managing municipal departments and department work queues")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @Operation(summary = "List all municipal departments", description = "Retrieves all active municipal departments with current open and resolved ticket counts")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Departments list successfully retrieved")
    })
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}/complaints")
    @Operation(summary = "Get complaints assigned to a department", description = "Retrieves a paginated list of complaints assigned to a specific department")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Department complaints retrieved"),
            @ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ResponseEntity<PageResponse<ComplaintResponse>> getDepartmentComplaints(
            @Parameter(description = "UUID of the department", required = true)
            @PathVariable UUID id,

            @Parameter(description = "Filter by complaint status")
            @RequestParam(required = false) ComplaintStatus status,

            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort property", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(departmentService.getDepartmentComplaints(id, status, pageable));
    }
}
