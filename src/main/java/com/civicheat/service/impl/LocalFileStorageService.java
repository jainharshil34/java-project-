package com.civicheat.service.impl;

import com.civicheat.config.CivicHeatProperties;
import com.civicheat.exception.FileStorageException;
import com.civicheat.exception.InvalidFileException;
import com.civicheat.exception.ResourceNotFoundException;
import com.civicheat.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Local filesystem implementation of FileStorageService with path sanitization and MIME validation.
 */
@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    public static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private static final Map<String, String> EXTENSION_MIME_MAP = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp"
    );

    private final Path storageLocation;

    public LocalFileStorageService(CivicHeatProperties properties) {
        this.storageLocation = Paths.get(properties.getUpload().getDir())
                .toAbsolutePath()
                .normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.storageLocation);
            log.info("Initialized local upload directory at: {}", this.storageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not initialize local upload directory", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Cannot store empty or missing file");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidFileException("File exceeds maximum allowed size of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new InvalidFileException("Invalid file format. Only JPEG, PNG, and WebP images are allowed.");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg"
        );

        if (originalFilename.contains("..")) {
            throw new InvalidFileException("Filename contains invalid relative path sequence: " + originalFilename);
        }

        String extension = getFileExtension(originalFilename, contentType);
        String uniqueFilename = UUID.randomUUID() + "." + extension;

        Path targetLocation = this.storageLocation.resolve(uniqueFilename).normalize();

        // Path traversal validation check
        if (!targetLocation.startsWith(this.storageLocation)) {
            throw new InvalidFileException("Cannot store file outside current storage directory");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored uploaded file: {} -> {}", originalFilename, uniqueFilename);
            return uniqueFilename;
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store file " + uniqueFilename, ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new ResourceNotFoundException("File name cannot be empty");
        }

        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new InvalidFileException("Invalid file path specified: " + filename);
        }

        try {
            Path filePath = this.storageLocation.resolve(filename).normalize();
            if (!filePath.startsWith(this.storageLocation)) {
                throw new InvalidFileException("Cannot access file outside storage directory");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found: " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File not found: " + filename);
        }
    }

    @Override
    public void deleteFile(String filename) {
        if (filename == null || filename.isBlank() || filename.contains("..")) {
            return;
        }
        try {
            Path filePath = this.storageLocation.resolve(filename).normalize();
            if (filePath.startsWith(this.storageLocation)) {
                Files.deleteIfExists(filePath);
                log.debug("Deleted file: {}", filename);
            }
        } catch (IOException ex) {
            log.warn("Could not delete file {}: {}", filename, ex.getMessage());
        }
    }

    @Override
    public String getContentType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            String ext = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
            return EXTENSION_MIME_MAP.getOrDefault(ext, "application/octet-stream");
        }
        return "application/octet-stream";
    }

    private String getFileExtension(String filename, String contentType) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            String ext = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
            if (EXTENSION_MIME_MAP.containsKey(ext)) {
                return ext;
            }
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }
}
