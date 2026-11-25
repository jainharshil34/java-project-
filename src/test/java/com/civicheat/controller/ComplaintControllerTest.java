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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "citizen@civicheat.local", roles = "CITIZEN")
class ComplaintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Submit new complaint with image successfully")
    void createComplaint_success() throws Exception {
        ComplaintCreateRequest request = ComplaintCreateRequest.builder()
                .title("Dangerous Pothole on Main St")
                .description("Deep pothole near the central traffic signal damaging tires.")
                .category(IssueCategory.POTHOLE)
                .latitude(37.7749)
                .longitude(-122.4194)
                .address("100 Main Street")
                .build();

        MockMultipartFile complaintPart = new MockMultipartFile(
                "complaint",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        MockMultipartFile imagePart = new MockMultipartFile(
                "images",
                "pothole.jpg",
                "image/jpeg",
                "sample-image-bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/complaints")
                        .file(complaintPart)
                        .file(imagePart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Dangerous Pothole on Main St")))
                .andExpect(jsonPath("$.category", is("POTHOLE")))
                .andExpect(jsonPath("$.status", is("ASSIGNED")))
                .andExpect(jsonPath("$.departmentName", is("Roads & Infrastructure")))
                .andExpect(jsonPath("$.images", hasSize(1)))
                .andExpect(jsonPath("$.statusHistories", hasSize(1)));
    }

    @Test
    @DisplayName("Reject complaint with validation errors (title too short)")
    void createComplaint_validationFailure() throws Exception {
        ComplaintCreateRequest request = ComplaintCreateRequest.builder()
                .title("Bad") // < 5 chars
                .description("Short") // < 10 chars
                .category(IssueCategory.POTHOLE)
                .latitude(150.0) // Invalid lat > 90
                .longitude(-122.4194)
                .build();

        MockMultipartFile complaintPart = new MockMultipartFile(
                "complaint",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        mockMvc.perform(multipart("/api/v1/complaints")
                        .file(complaintPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.title", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.description", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.latitude", notNullValue()));
    }

    @Test
    @DisplayName("Query complaints with pagination and filters")
    void getComplaints_withPaginationAndFilters() throws Exception {
        // First create a complaint
        ComplaintCreateRequest request = ComplaintCreateRequest.builder()
                .title("Overflowing Garbage Bin")
                .description("Garbage bin has been overflowing for three consecutive days.")
                .category(IssueCategory.GARBAGE)
                .latitude(40.7128)
                .longitude(-74.0060)
                .build();

        MockMultipartFile complaintPart = new MockMultipartFile(
                "complaint",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        mockMvc.perform(multipart("/api/v1/complaints").file(complaintPart))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/complaints")
                        .param("category", "GARBAGE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].category", is("GARBAGE")));
    }

    @Test
    @DisplayName("Upvote complaint enforces one vote per user")
    void upvoteComplaint_enforcesOneVotePerUser() throws Exception {
        ComplaintCreateRequest request = ComplaintCreateRequest.builder()
                .title("Broken Streetlight at Corner")
                .description("Streetlight has been flickering and completely turned off.")
                .category(IssueCategory.STREETLIGHT)
                .latitude(12.9716)
                .longitude(77.5946)
                .build();

        MockMultipartFile complaintPart = new MockMultipartFile(
                "complaint",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        String responseJson = mockMvc.perform(multipart("/api/v1/complaints").file(complaintPart))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String complaintId = objectMapper.readTree(responseJson).get("id").asText();

        // 1st Upvote -> 200 OK
        mockMvc.perform(post("/api/v1/complaints/" + complaintId + "/upvote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upvoteCount", is(1)));

        // 2nd Upvote from same user -> 400 Bad Request
        mockMvc.perform(post("/api/v1/complaints/" + complaintId + "/upvote"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already upvoted")));
    }

    @Test
    @DisplayName("Duplicate submission links to original, increments upvotes, and returns duplicate flag")
    void createComplaint_duplicateDetected_linksToOriginal() throws Exception {
        ComplaintCreateRequest firstReq = ComplaintCreateRequest.builder()
                .title("Severe water pipeline burst")
                .description("Drinking water pipeline burst flooding the whole main road.")
                .category(IssueCategory.WATER_LEAK)
                .latitude(12.971600)
                .longitude(77.594600)
                .build();

        MockMultipartFile part1 = new MockMultipartFile(
                "complaint", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(firstReq)
        );

        String firstResponse = mockMvc.perform(multipart("/api/v1/complaints").file(part1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("ASSIGNED")))
                .andExpect(jsonPath("$.duplicate", is(false)))
                .andReturn().getResponse().getContentAsString();

        String originalId = objectMapper.readTree(firstResponse).get("id").asText();

        // 2nd Complaint at exact same location with nearly identical description
        ComplaintCreateRequest duplicateReq = ComplaintCreateRequest.builder()
                .title("Water pipeline burst")
                .description("Drinking water pipeline burst flooding the main road.")
                .category(IssueCategory.WATER_LEAK)
                .latitude(12.971600)
                .longitude(77.594600)
                .build();

        MockMultipartFile part2 = new MockMultipartFile(
                "complaint", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(duplicateReq)
        );

        mockMvc.perform(multipart("/api/v1/complaints").file(part2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DUPLICATE")))
                .andExpect(jsonPath("$.duplicate", is(true)))
                .andExpect(jsonPath("$.duplicateOfId", is(originalId)));

        // Verify original complaint's upvote count incremented to 1
        mockMvc.perform(get("/api/v1/complaints/" + originalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upvoteCount", is(1)));
    }
}
