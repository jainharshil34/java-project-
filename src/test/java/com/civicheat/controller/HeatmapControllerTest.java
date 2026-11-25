package com.civicheat.controller;

import com.civicheat.dto.request.ComplaintCreateRequest;
import com.civicheat.enums.IssueCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class HeatmapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Fetch heatmap points, hotspots, and analytics statistics")
    void getHeatmapPointsAndStats() throws Exception {
        // Create sample complaints
        ComplaintCreateRequest req1 = ComplaintCreateRequest.builder()
                .title("Broken Streetlight High Risk")
                .description("Dangerous streetlight fallen on the street near children playground")
                .category(IssueCategory.STREETLIGHT)
                .latitude(12.971600)
                .longitude(77.594600)
                .build();

        ComplaintCreateRequest req2 = ComplaintCreateRequest.builder()
                .title("Major Water leakage on road")
                .description("Water leakage flooding road and causing traffic hazard")
                .category(IssueCategory.WATER_LEAK)
                .latitude(12.971620)
                .longitude(77.594620)
                .build();

        MockMultipartFile part1 = new MockMultipartFile("complaint", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(req1));
        MockMultipartFile part2 = new MockMultipartFile("complaint", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(req2));

        mockMvc.perform(multipart("/api/v1/complaints").file(part1)).andExpect(status().isCreated());
        mockMvc.perform(multipart("/api/v1/complaints").file(part2)).andExpect(status().isCreated());

        // 1. Heatmap points endpoint
        mockMvc.perform(get("/api/v1/heatmap/points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[0]", hasSize(3))); // [lat, lng, weight]

        // 2. Heatmap hotspots endpoint
        mockMvc.perform(get("/api/v1/heatmap/hotspots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].centroidLat", notNullValue()))
                .andExpect(jsonPath("$[0].heatIntensity", notNullValue()));

        // 3. Platform stats endpoint
        mockMvc.perform(get("/api/v1/heatmap/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalComplaints", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.categoryCounts", notNullValue()))
                .andExpect(jsonPath("$.statusCounts", notNullValue()));
    }
}
