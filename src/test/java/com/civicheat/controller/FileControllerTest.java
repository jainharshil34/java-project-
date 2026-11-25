package com.civicheat.controller;

import com.civicheat.service.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileStorageService fileStorageService;

    @Test
    @DisplayName("GET /api/v1/files/{filename} serves stored media file")
    void getFile_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "images",
                "test-incident.jpg",
                "image/jpeg",
                "test image content binary data".getBytes()
        );

        String storedFilename = fileStorageService.storeFile(file);

        mockMvc.perform(get("/api/v1/files/" + storedFilename))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes("test image content binary data".getBytes()));
    }

    @Test
    @DisplayName("GET /api/v1/files/{filename} returns 404 for non-existent file")
    void getFile_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/files/non-existent-image-uuid.jpg"))
                .andExpect(status().isNotFound());
    }
}
