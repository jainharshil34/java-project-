package com.civicheat.controller;

import com.civicheat.dto.request.ComplaintCreateRequest;
import com.civicheat.entity.Department;
import com.civicheat.enums.IssueCategory;
import com.civicheat.repository.DepartmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "citizen@civicheat.local", roles = "CITIZEN")
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Department sanitationDept;

    @BeforeEach
    void setUp() {
        if (departmentRepository.findByName("Sanitation").isEmpty()) {
            sanitationDept = departmentRepository.save(
                    Department.builder()
                            .name("Sanitation")
                            .code("SANITATION")
                            .description("Sanitation department")
                            .contactEmail("sanitation@civic.gov")
                            .build()
            );
        } else {
            sanitationDept = departmentRepository.findByName("Sanitation").get();
        }

        if (departmentRepository.findByName("Roads & Infrastructure").isEmpty()) {
            departmentRepository.save(
                    Department.builder()
                            .name("Roads & Infrastructure")
                            .code("ROADS_INFRA")
                            .description("Roads department")
                            .contactEmail("roads@civic.gov")
                            .build()
            );
        }
    }

    @Test
    @DisplayName("List all municipal departments")
    void getAllDepartments_success() throws Exception {
        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", notNullValue()))
                .andExpect(jsonPath("$[0].code", notNullValue()));
    }

    @Test
    @DisplayName("Auto-route complaint to Sanitation and query department queue")
    void autoRouteAndQueryDepartmentQueue() throws Exception {
        ComplaintCreateRequest request = ComplaintCreateRequest.builder()
                .title("Sidewalk Garbage Dump")
                .description("Illegal dumping of household waste blocking the pedestrian sidewalk.")
                .category(IssueCategory.GARBAGE)
                .latitude(12.9716)
                .longitude(77.5946)
                .build();

        MockMultipartFile complaintPart = new MockMultipartFile(
                "complaint", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request)
        );

        // Submit complaint -> should be ASSIGNED to Sanitation
        mockMvc.perform(multipart("/api/v1/complaints").file(complaintPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("ASSIGNED")))
                .andExpect(jsonPath("$.departmentName", is("Sanitation")));

        // Query department complaints
        mockMvc.perform(get("/api/v1/departments/" + sanitationDept.getId() + "/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }
}
