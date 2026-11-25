package com.civicheat.service.impl;

import com.civicheat.dto.response.ComplaintResponse;
import com.civicheat.dto.response.DepartmentResponse;
import com.civicheat.dto.response.PageResponse;
import com.civicheat.entity.Complaint;
import com.civicheat.entity.Department;
import com.civicheat.enums.ComplaintStatus;
import com.civicheat.exception.ResourceNotFoundException;
import com.civicheat.mapper.ComplaintMapper;
import com.civicheat.mapper.DepartmentMapper;
import com.civicheat.repository.ComplaintRepository;
import com.civicheat.repository.DepartmentRepository;
import com.civicheat.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Production implementation of DepartmentService for department metrics and workflow queues.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final ComplaintRepository complaintRepository;
    private final DepartmentMapper departmentMapper;
    private final ComplaintMapper complaintMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        List<Department> departments = departmentRepository.findAll();
        return departments.stream().map(dept -> {
            long openCount = complaintRepository.countByDepartmentIdAndStatus(dept.getId(), ComplaintStatus.ASSIGNED)
                    + complaintRepository.countByDepartmentIdAndStatus(dept.getId(), ComplaintStatus.IN_PROGRESS)
                    + complaintRepository.countByDepartmentIdAndStatus(dept.getId(), ComplaintStatus.SUBMITTED);

            long resolvedCount = complaintRepository.countByDepartmentIdAndStatus(dept.getId(), ComplaintStatus.RESOLVED);

            return departmentMapper.toResponse(dept, openCount, resolvedCount);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ComplaintResponse> getDepartmentComplaints(
            UUID departmentId, ComplaintStatus status, Pageable pageable) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found with ID: " + departmentId);
        }

        Page<Complaint> page;
        if (status != null) {
            page = complaintRepository.findByDepartmentIdAndStatus(departmentId, status, pageable);
        } else {
            page = complaintRepository.findByDepartmentId(departmentId, pageable);
        }

        return PageResponse.from(page.map(complaintMapper::toResponse));
    }
}
