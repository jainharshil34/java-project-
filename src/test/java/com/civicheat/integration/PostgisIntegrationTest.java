package com.civicheat.integration;

import com.civicheat.dto.request.ComplaintCreateRequest;
import com.civicheat.dto.request.LoginRequest;
import com.civicheat.dto.request.RegisterRequest;
import com.civicheat.dto.response.AuthResponse;
import com.civicheat.dto.response.ComplaintDetailResponse;
import com.civicheat.dto.response.HotspotResponse;
import com.civicheat.enums.ComplaintStatus;
import com.civicheat.enums.IssueCategory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PostgisIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgis = new PostgreSQLContainer<>("postgis/postgis:16-3.4")
            .withDatabaseName("civicheat_testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgis::getJdbcUrl);
        registry.add("spring.datasource.username", postgis::getUsername);
        registry.add("spring.datasource.password", postgis::getPassword);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.spatial.dialect.postgis.PostgisDialect");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("civicheat.seeder.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("End-to-end flow with PostGIS: Register -> Login -> Submit -> Duplicate Detection -> Routing -> Heatmap")
    void fullIntegrationFlow_withPostgis() throws Exception {
        // 1. Citizen Registration
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("postgis.citizen@civicheat.local")
                .password("SecurePass123!")
                .fullName("PostGIS Citizen")
                .phone("+1-555-0144")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists());

        // 2. Citizen Login
        LoginRequest loginReq = LoginRequest.builder()
                .email("postgis.citizen@civicheat.local")
                .password("SecurePass123!")
                .build();

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        AuthResponse auth = objectMapper.readValue(loginResponse, AuthResponse.class);
        String token = auth.getToken();
        assertThat(token).isNotBlank();

        // 3. Submit Initial Complaint (Pothole at 37.774900, -122.419400)
        ComplaintCreateRequest initialReq = ComplaintCreateRequest.builder()
                .title("Severe pothole on Market St")
                .description("Dangerous deep asphalt pothole damaging vehicle rims near pedestrian crossing")
                .category(IssueCategory.POTHOLE)
                .latitude(37.774900)
                .longitude(-122.419400)
                .address("Market & 5th St")
                .build();

        MockMultipartFile initialPart = new MockMultipartFile(
                "complaint", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(initialReq)
        );

        String initialRespJson = mockMvc.perform(multipart("/api/v1/complaints")
                        .file(initialPart)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.departmentName").value("Roads & Infrastructure"))
                .andExpect(jsonPath("$.duplicate").value(false))
                .andExpect(jsonPath("$.severityScore").isNumber())
                .andReturn().getResponse().getContentAsString();

        ComplaintDetailResponse initialComplaint = objectMapper.readValue(initialRespJson, ComplaintDetailResponse.class);
        assertThat(initialComplaint.getId()).isNotNull();
        assertThat(initialComplaint.getSeverityScore()).isGreaterThan(0);

        // 4. Submit Duplicate Complaint (~15m away with matching category & tokens)
        ComplaintCreateRequest duplicateReq = ComplaintCreateRequest.builder()
                .title("Pothole on Market St")
                .description("Dangerous asphalt pothole damaging vehicle rims near pedestrian crossing")
                .category(IssueCategory.POTHOLE)
                .latitude(37.774950)
                .longitude(-122.419420)
                .build();

        MockMultipartFile dupPart = new MockMultipartFile(
                "complaint", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(duplicateReq)
        );

        mockMvc.perform(multipart("/api/v1/complaints")
                        .file(dupPart)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DUPLICATE"))
                .andExpect(jsonPath("$.duplicate").value(true))
                .andExpect(jsonPath("$.duplicateOfId").value(initialComplaint.getId().toString()));

        // 5. Verify Original Complaint Upvote Count Incremented
        mockMvc.perform(get("/api/v1/complaints/" + initialComplaint.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upvoteCount").value(1));

        // 6. Query Heatmap Points Endpoint
        mockMvc.perform(get("/api/v1/heatmap/points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 7. Query Hotspots Endpoint
        String hotspotsJson = mockMvc.perform(get("/api/v1/heatmap/hotspots"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<HotspotResponse> hotspots = objectMapper.readValue(hotspotsJson, new TypeReference<>() {});
        assertThat(hotspots).isNotNull();
    }
}
