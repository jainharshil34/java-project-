package com.civicheat.service;

import com.civicheat.dto.response.ComplaintResponse;
import com.civicheat.dto.response.DepartmentResponse;
import com.civicheat.dto.response.PageResponse;
import com.civicheat.enums.ComplaintStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface governing municipal department management and department complaint routing queues.
 */
public interface DepartmentService {

    /**
     * Retrieves all configured municipal departments with current active workload counts.
     */
    List<DepartmentResponse> getAllDepartments();

    /**
     * Retrieves paginated complaints assigned to a specific department.
     *
     * @param departmentId Department UUID
     * @param status       Optional status filter
     * @param pageable     Pagination and sorting parameters
     * @return Paginated complaints assigned to the department
     */
    PageResponse<ComplaintResponse> getDepartmentComplaints(UUID departmentId, ComplaintStatus status, Pageable pageable);
}
