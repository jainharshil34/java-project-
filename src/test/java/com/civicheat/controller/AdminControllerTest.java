package com.civicheat.controller;

import com.civicheat.dto.request.CreateOfficerRequest;
import com.civicheat.dto.request.ReassignComplaintRequest;
import com.civicheat.dto.request.UpdateComplaintStatusRequest;
import com.civicheat.entity.Complaint;
import com.civicheat.entity.Department;
import com.civicheat.entity.User;
import com.civicheat.enums.ComplaintStatus;
import com.civicheat.enums.IssueCategory;
import com.civicheat.enums.Role;
import com.civicheat.enums.SeverityLevel;
import com.civicheat.repository.ComplaintRepository;
import com.civicheat.repository.DepartmentRepository;
import com.civicheat.repository.UserRepository;
import com.civicheat.security.JwtTokenProvider;
import com.civicheat.util.GeoUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Department deptRoads;
    private Department deptSanitation;
    private User adminUser;
    private User roadsOfficer;
    private User sanitationOfficer;
    private User citizenUser;
    private String adminToken;
    private String roadsOfficerToken;
    private String sanitationOfficerToken;
    private String citizenToken;

    @BeforeEach
    void setUp() {
        complaintRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();

        deptRoads = departmentRepository.save(Department.builder()
                .name("Roads & Infrastructure")
                .code("ROADS_INFRA")
                .contactEmail("roads@civicheat.local")
                .build());

        deptSanitation = departmentRepository.save(Department.builder()
                .name("Sanitation")
                .code("SANITATION")
                .contactEmail("sanitation@civicheat.local")
                .build());

        adminUser = userRepository.save(User.builder()
                .email("admin@civicheat.gov")
                .password(passwordEncoder.encode("AdminPass123!"))
                .fullName("Platform Administrator")
                .role(Role.ADMIN)
                .build());

        roadsOfficer = userRepository.save(User.builder()
                .email("roads.officer@civicheat.gov")
                .password(passwordEncoder.encode("OfficerPass123!"))
                .fullName("Roads Officer")
                .role(Role.OFFICER)
                .department(deptRoads)
                .build());

        sanitationOfficer = userRepository.save(User.builder()
                .email("sanitation.officer@civicheat.gov")
                .password(passwordEncoder.encode("OfficerPass123!"))
                .fullName("Sanitation Officer")
                .role(Role.OFFICER)
                .department(deptSanitation)
                .build());

        citizenUser = userRepository.save(User.builder()
                .email("citizen@civicheat.local")
                .password(passwordEncoder.encode("CitizenPass123!"))
                .fullName("John Citizen")
                .role(Role.CITIZEN)
                .build());

        adminToken = jwtTokenProvider.generateToken(adminUser);
        roadsOfficerToken = jwtTokenProvider.generateToken(roadsOfficer);
        sanitationOfficerToken = jwtTokenProvider.generateToken(sanitationOfficer);
        citizenToken = jwtTokenProvider.generateToken(citizenUser);
    }

    private Complaint createTestComplaint(Department dept, ComplaintStatus status) {
        return complaintRepository.save(Complaint.builder()
                .title("Damaged road on 5th Ave")
                .description("Deep pothole disrupting traffic near pedestrian crossing")
                .category(IssueCategory.POTHOLE)
                .latitude(37.7749)
                .longitude(-122.4194)
                .location(GeoUtils.createPoint(37.7749, -122.4194))
                .status(status)
                .severityLevel(SeverityLevel.HIGH)
                .severityScore(70)
                .reporter(citizenUser)
                .department(dept)
                .build());
    }

    @Test
    @DisplayName("PATCH /api/v1/complaints/{id}/status - Officer updates complaint in their own department successfully")
    void updateStatus_officerOwnDepartment_success() throws Exception {
        Complaint complaint = createTestComplaint(deptRoads, ComplaintStatus.ASSIGNED);

        UpdateComplaintStatusRequest request = UpdateComplaintStatusRequest.builder()
                .status(ComplaintStatus.IN_PROGRESS)
                .remark("Dispatching repair crew")
                .build();

        mockMvc.perform(patch("/api/v1/complaints/" + complaint.getId() + "/status")
                        .header("Authorization", "Bearer " + roadsOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
    }

    @Test
    @DisplayName("PATCH /api/v1/complaints/{id}/status - Officer updating complaint in another department returns 403 Forbidden")
    void updateStatus_officerOtherDepartment_returnsForbidden() throws Exception {
        Complaint complaint = createTestComplaint(deptSanitation, ComplaintStatus.ASSIGNED);

        UpdateComplaintStatusRequest request = UpdateComplaintStatusRequest.builder()
                .status(ComplaintStatus.IN_PROGRESS)
                .remark("Cross-department attempt")
                .build();

        mockMvc.perform(patch("/api/v1/complaints/" + complaint.getId() + "/status")
                        .header("Authorization", "Bearer " + roadsOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/v1/complaints/{id}/status - Citizen attempting status update returns 403 Forbidden")
    void updateStatus_citizenRole_returnsForbidden() throws Exception {
        Complaint complaint = createTestComplaint(deptRoads, ComplaintStatus.ASSIGNED);

        UpdateComplaintStatusRequest request = UpdateComplaintStatusRequest.builder()
                .status(ComplaintStatus.RESOLVED)
                .build();

        mockMvc.perform(patch("/api/v1/complaints/" + complaint.getId() + "/status")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/v1/complaints/{id}/status - Admin can resolve complaint across any department and sets resolvedAt")
    void updateStatus_admin_resolvesAndSetsResolvedAt() throws Exception {
        Complaint complaint = createTestComplaint(deptSanitation, ComplaintStatus.IN_PROGRESS);

        UpdateComplaintStatusRequest request = UpdateComplaintStatusRequest.builder()
                .status(ComplaintStatus.RESOLVED)
                .remark("Work completed and verified by city inspector")
                .build();

        mockMvc.perform(patch("/api/v1/complaints/" + complaint.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESOLVED")))
                .andExpect(jsonPath("$.resolvedAt", notNullValue()));
    }

    @Test
    @DisplayName("PATCH /api/v1/complaints/{id}/status - Invalid status transition returns 400 Bad Request")
    void updateStatus_invalidTransition_returnsBadRequest() throws Exception {
        Complaint complaint = createTestComplaint(deptRoads, ComplaintStatus.RESOLVED);

        // Transition from RESOLVED directly to SUBMITTED is invalid
        UpdateComplaintStatusRequest request = UpdateComplaintStatusRequest.builder()
                .status(ComplaintStatus.SUBMITTED)
                .build();

        mockMvc.perform(patch("/api/v1/complaints/" + complaint.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/admin/officers - Admin creates a new department officer")
    void createOfficer_adminRole_success() throws Exception {
        CreateOfficerRequest request = CreateOfficerRequest.builder()
                .email("new.officer@civicheat.gov")
                .password("Officer123!")
                .fullName("New Officer")
                .phone("+1-555-0155")
                .departmentId(deptRoads.getId())
                .build();

        mockMvc.perform(post("/api/v1/admin/officers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("new.officer@civicheat.gov")))
                .andExpect(jsonPath("$.role", is("OFFICER")))
                .andExpect(jsonPath("$.departmentName", is("Roads & Infrastructure")));
    }

    @Test
    @DisplayName("POST /api/v1/admin/officers - Citizen attempting to create officer returns 403 Forbidden")
    void createOfficer_citizenRole_returnsForbidden() throws Exception {
        CreateOfficerRequest request = CreateOfficerRequest.builder()
                .email("illegal.officer@civicheat.gov")
                .password("Officer123!")
                .fullName("Illegal Officer")
                .departmentId(deptRoads.getId())
                .build();

        mockMvc.perform(post("/api/v1/admin/officers")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/complaints/{id}/reassign - Admin reassigns complaint department")
    void reassignComplaint_adminRole_success() throws Exception {
        Complaint complaint = createTestComplaint(deptRoads, ComplaintStatus.ASSIGNED);

        ReassignComplaintRequest request = ReassignComplaintRequest.builder()
                .departmentId(deptSanitation.getId())
                .reason("Debris removal requires sanitation team")
                .build();

        mockMvc.perform(post("/api/v1/complaints/" + complaint.getId() + "/reassign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId", is(deptSanitation.getId().toString())))
                .andExpect(jsonPath("$.departmentName", is("Sanitation")));
    }
}
