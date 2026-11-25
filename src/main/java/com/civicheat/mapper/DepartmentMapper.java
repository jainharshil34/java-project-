package com.civicheat.mapper;

import com.civicheat.dto.response.DepartmentResponse;
import com.civicheat.entity.Department;
import org.springframework.stereotype.Component;

/**
 * Mapper translating between Department domain entities and API DTOs.
 */
@Component
public class DepartmentMapper {

    public DepartmentResponse toResponse(Department department, Long openCount, Long resolvedCount) {
        if (department == null) {
            return null;
        }

        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .code(department.getCode())
                .description(department.getDescription())
                .contactEmail(department.getContactEmail())
                .contactPhone(department.getContactPhone())
                .openComplaintsCount(openCount != null ? openCount : 0L)
                .resolvedComplaintsCount(resolvedCount != null ? resolvedCount : 0L)
                .createdAt(department.getCreatedAt())
                .build();
    }
}
