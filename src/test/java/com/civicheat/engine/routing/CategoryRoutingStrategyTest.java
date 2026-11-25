package com.civicheat.engine.routing;

import com.civicheat.config.CivicHeatProperties;
import com.civicheat.entity.Complaint;
import com.civicheat.entity.Department;
import com.civicheat.enums.IssueCategory;
import com.civicheat.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryRoutingStrategyTest {

    @Mock
    private DepartmentRepository departmentRepository;

    private CivicHeatProperties properties;
    private CategoryRoutingStrategy routingStrategy;

    @BeforeEach
    void setUp() {
        properties = new CivicHeatProperties();
        routingStrategy = new CategoryRoutingStrategy(departmentRepository, properties);
        routingStrategy.init();
    }

    @ParameterizedTest
    @CsvSource({
            "POTHOLE, Roads & Infrastructure",
            "GARBAGE, Sanitation",
            "STREETLIGHT, Electricity",
            "WATER_LEAK, Water Supply",
            "NOISE, Pollution Control"
    })
    @DisplayName("Route each issue category to the expected municipal department")
    void route_allCategories(IssueCategory category, String expectedDepartmentName) {
        Department expectedDept = Department.builder()
                .id(UUID.randomUUID())
                .name(expectedDepartmentName)
                .code(expectedDepartmentName.toUpperCase().replace(" ", "_").replace("&", "AND"))
                .build();

        when(departmentRepository.findByName(eq(expectedDepartmentName)))
                .thenReturn(Optional.of(expectedDept));

        Complaint complaint = Complaint.builder()
                .id(UUID.randomUUID())
                .category(category)
                .title("Test issue")
                .description("Test description")
                .build();

        Department routed = routingStrategy.route(complaint);

        assertThat(routed).isNotNull();
        assertThat(routed.getName()).isEqualTo(expectedDepartmentName);
    }

    @Test
    @DisplayName("Fallback to default department when category department is not found in database")
    void route_fallbackWhenNotFoundInDb() {
        Department fallbackDept = Department.builder()
                .id(UUID.randomUUID())
                .name("Roads & Infrastructure")
                .code("ROADS_INFRA")
                .build();

        when(departmentRepository.findByName("Water Supply")).thenReturn(Optional.empty());
        when(departmentRepository.findByName("Roads & Infrastructure")).thenReturn(Optional.of(fallbackDept));

        Complaint complaint = Complaint.builder()
                .id(UUID.randomUUID())
                .category(IssueCategory.WATER_LEAK)
                .build();

        Department routed = routingStrategy.route(complaint);

        assertThat(routed).isNotNull();
        assertThat(routed.getName()).isEqualTo("Roads & Infrastructure");
    }

    @Test
    @DisplayName("Fallback to default department when complaint or category is null")
    void route_nullComplaint_returnsFallback() {
        Department fallbackDept = Department.builder()
                .id(UUID.randomUUID())
                .name("Roads & Infrastructure")
                .code("ROADS_INFRA")
                .build();

        when(departmentRepository.findByName("Roads & Infrastructure")).thenReturn(Optional.of(fallbackDept));

        Department routed = routingStrategy.route(null);

        assertThat(routed).isNotNull();
        assertThat(routed.getName()).isEqualTo("Roads & Infrastructure");
    }
}
