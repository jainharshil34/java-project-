package com.civicheat.controller;

import com.civicheat.dto.request.LoginRequest;
import com.civicheat.dto.request.RegisterRequest;
import com.civicheat.entity.User;
import com.civicheat.enums.Role;
import com.civicheat.repository.UserRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register successfully creates a citizen account with JWT token")
    void register_success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("alice.citizen@civicheat.local")
                .password("Password123!")
                .fullName("Alice Citizen")
                .phone("+1-555-0101")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.email", is("alice.citizen@civicheat.local")))
                .andExpect(jsonPath("$.user.fullName", is("Alice Citizen")))
                .andExpect(jsonPath("$.user.role", is("CITIZEN")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with existing email returns 409 Conflict")
    void register_duplicateEmail_returnsConflict() throws Exception {
        User existing = User.builder()
                .email("duplicate@civicheat.local")
                .password(passwordEncoder.encode("Secret123!"))
                .fullName("Existing User")
                .role(Role.CITIZEN)
                .build();
        userRepository.save(existing);

        RegisterRequest request = RegisterRequest.builder()
                .email("duplicate@civicheat.local")
                .password("NewPassword123!")
                .fullName("Another Name")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with valid credentials returns JWT token")
    void login_success() throws Exception {
        User user = User.builder()
                .email("bob@civicheat.local")
                .password(passwordEncoder.encode("SecretPass123!"))
                .fullName("Bob Miller")
                .role(Role.CITIZEN)
                .build();
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .email("bob@civicheat.local")
                .password("SecretPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email", is("bob@civicheat.local")))
                .andExpect(jsonPath("$.user.role", is("CITIZEN")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with invalid password returns 401 Unauthorized")
    void login_badCredentials_returnsUnauthorized() throws Exception {
        User user = User.builder()
                .email("bob@civicheat.local")
                .password(passwordEncoder.encode("SecretPass123!"))
                .fullName("Bob Miller")
                .role(Role.CITIZEN)
                .build();
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .email("bob@civicheat.local")
                .password("WrongPassword")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me with valid Bearer token returns authenticated user profile")
    void getMe_authenticated_returnsProfile() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("charlie@civicheat.local")
                .password("Password123!")
                .fullName("Charlie Brown")
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(responseJson).get("token").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("charlie@civicheat.local")))
                .andExpect(jsonPath("$.fullName", is("Charlie Brown")))
                .andExpect(jsonPath("$.role", is("CITIZEN")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me without token returns 403 or 401 Forbidden")
    void getMe_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isForbidden());
    }
}
