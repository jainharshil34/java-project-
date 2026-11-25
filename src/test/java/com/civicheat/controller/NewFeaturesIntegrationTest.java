package com.civicheat.controller;

import com.civicheat.dto.request.AddCommentRequest;
import com.civicheat.dto.request.ComplaintCreateRequest;
import com.civicheat.enums.IssueCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NewFeaturesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "citizen@civicheat.local", roles = "CITIZEN")
    @DisplayName("Comments, Search, CSV Export, Analytics, and Notifications work end-to-end")
    void testNewFeaturesEndToEnd() throws Exception {
        ComplaintCreateRequest req = ComplaintCreateRequest.builder()
                .title("Broken Water Main on Elm Street")
                .description("Severe pipe burst flooding the entire crosswalk")
                .category(IssueCategory.WATER_LEAK)
                .latitude(28.6139)
                .longitude(77.2090)
                .build();

        MockMultipartFile complaintPart = new MockMultipartFile(
                "complaint", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(req));

        MvcResult createResult = mockMvc.perform(multipart("/api/v1/complaints").file(complaintPart))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String complaintId = createdJson.get("id").asText();

        // 1. Add a comment
        AddCommentRequest commentReq = new AddCommentRequest();
        commentReq.setContent("Crews should prioritize this before rush hour.");
        mockMvc.perform(post("/api/v1/complaints/{id}/comments", complaintId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content", is("Crews should prioritize this before rush hour.")))
                .andExpect(jsonPath("$.isOfficial", is(false)));

        // 2. Fetch comments
        mockMvc.perform(get("/api/v1/complaints/{id}/comments", complaintId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        // 3. Full-text search
        mockMvc.perform(get("/api/v1/search/complaints").param("q", "Elm Street"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].title", containsString("Elm Street")));

        // 4. CSV Export
        mockMvc.perform(get("/api/v1/export/complaints.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(content().string(containsString("Broken Water Main on Elm Street")));

        // 5. Analytics endpoints
        mockMvc.perform(get("/api/v1/analytics/resolution-rate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalComplaints", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/analytics/daily-trend").param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days", is(7)));

        mockMvc.perform(get("/api/v1/analytics/status-breakdown"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/analytics/severity-breakdown"))
                .andExpect(status().isOk());

        // 6. Notifications endpoints
        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", greaterThanOrEqualTo(0)));

        mockMvc.perform(post("/api/v1/notifications/mark-all-read"))
                .andExpect(status().isOk());
    }
}
