package com.civicheat.engine.routing;

import com.civicheat.config.CivicHeatProperties;
import com.civicheat.entity.Complaint;
import com.civicheat.entity.Department;
import com.civicheat.enums.IssueCategory;
import com.civicheat.repository.DepartmentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Category-based department routing strategy mapping issue categories directly to municipal departments.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryRoutingStrategy implements RoutingStrategy {

    private final DepartmentRepository departmentRepository;
    private final CivicHeatProperties properties;

    private final Map<IssueCategory, String> categoryDepartmentMap = new EnumMap<>(IssueCategory.class);

    @PostConstruct
    public void init() {
        categoryDepartmentMap.put(IssueCategory.POTHOLE, "Roads & Infrastructure");
        categoryDepartmentMap.put(IssueCategory.GARBAGE, "Sanitation");
        categoryDepartmentMap.put(IssueCategory.STREETLIGHT, "Electricity");
        categoryDepartmentMap.put(IssueCategory.WATER_LEAK, "Water Supply");
        categoryDepartmentMap.put(IssueCategory.NOISE, "Pollution Control");
        log.info("Initialized CategoryRoutingStrategy with {} category mappings", categoryDepartmentMap.size());
    }

    @Override
    public Department route(Complaint complaint) {
        if (complaint == null || complaint.getCategory() == null) {
            return resolveFallbackDepartment();
        }

        String targetDepartmentName = categoryDepartmentMap.get(complaint.getCategory());
        if (targetDepartmentName != null) {
            Optional<Department> departmentOpt = departmentRepository.findByName(targetDepartmentName);
            if (departmentOpt.isPresent()) {
                Department department = departmentOpt.get();
                log.info("Complaint category {} routed to Department: {} ({})",
                        complaint.getCategory(), department.getName(), department.getId());
                return department;
            }
            log.warn("Configured department '{}' not found in database for category {}. Falling back to default.",
                    targetDepartmentName, complaint.getCategory());
        }

        return resolveFallbackDepartment();
    }

    private Department resolveFallbackDepartment() {
        String fallbackName = properties.getRouting().getDefaultDepartmentName();
        return departmentRepository.findByName(fallbackName)
                .orElseGet(() -> departmentRepository.findAll().stream().findFirst()
                        .orElseGet(() -> departmentRepository.save(
                                Department.builder()
                                        .name(fallbackName)
                                        .code(properties.getRouting().getDefaultDepartmentCode())
                                        .description("Default municipal department fallback")
                                        .contactEmail("admin@civic.gov")
                                        .build()
                        )));
    }
}
