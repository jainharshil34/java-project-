package com.civicheat.service;

import com.civicheat.config.CivicHeatProperties;
import com.civicheat.exception.InvalidFileException;
import com.civicheat.exception.ResourceNotFoundException;
import com.civicheat.service.impl.LocalFileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempUploadDir;

    private LocalFileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        CivicHeatProperties properties = new CivicHeatProperties();
        properties.getUpload().setDir(tempUploadDir.toString());

        fileStorageService = new LocalFileStorageService(properties);
        fileStorageService.init();
    }

    @Test
    @DisplayName("Successfully store and retrieve valid JPEG file")
    void storeAndLoadFile_validJpeg_success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pothole.jpg",
                "image/jpeg",
                "dummy image content".getBytes()
        );

        String filename = fileStorageService.storeFile(file);

        assertThat(filename).endsWith(".jpg");
        Resource resource = fileStorageService.loadFileAsResource(filename);
        assertThat(resource.exists()).isTrue();
        assertThat(resource.contentLength()).isEqualTo("dummy image content".getBytes().length);
    }

    @Test
    @DisplayName("Successfully store and retrieve valid WebP file")
    void storeAndLoadFile_validWebP_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "garbage.webp",
                "image/webp",
                "webp content".getBytes()
        );

        String filename = fileStorageService.storeFile(file);

        assertThat(filename).endsWith(".webp");
        Resource resource = fileStorageService.loadFileAsResource(filename);
        assertThat(resource.exists()).isTrue();
    }

    @Test
    @DisplayName("Reject invalid content types (e.g. PDF or executable)")
    void storeFile_invalidContentType_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        assertThatThrownBy(() -> fileStorageService.storeFile(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Only JPEG, PNG, and WebP images are allowed");
    }

    @Test
    @DisplayName("Reject file path traversal in filename")
    void loadFile_pathTraversal_throwsException() {
        assertThatThrownBy(() -> fileStorageService.loadFileAsResource("../secret.txt"))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    @DisplayName("Throw ResourceNotFoundException for missing file")
    void loadFile_notFound_throwsException() {
        assertThatThrownBy(() -> fileStorageService.loadFileAsResource("non-existent-file.jpg"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Get MIME content type by filename extension")
    void getContentType_returnsCorrectMime() {
        assertThat(fileStorageService.getContentType("photo.png")).isEqualTo("image/png");
        assertThat(fileStorageService.getContentType("photo.jpeg")).isEqualTo("image/jpeg");
        assertThat(fileStorageService.getContentType("photo.webp")).isEqualTo("image/webp");
        assertThat(fileStorageService.getContentType("file.unknown")).isEqualTo("application/octet-stream");
    }
}
