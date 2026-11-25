package com.civicheat.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service contract for storing, retrieving, and validating uploaded civic complaint attachments.
 */
public interface FileStorageService {

    /**
     * Validates and saves an uploaded multipart file to storage.
     *
     * @param file The multipart file to store
     * @return Generated unique file name
     */
    String storeFile(MultipartFile file);

    /**
     * Loads a stored file as a Spring Resource.
     *
     * @param filename Name of the stored file
     * @return Resource representing the file
     */
    Resource loadFileAsResource(String filename);

    /**
     * Deletes a file from storage if present.
     *
     * @param filename Name of the file to delete
     */
    void deleteFile(String filename);

    /**
     * Detects or infers content type from filename.
     *
     * @param filename Name of the stored file
     * @return MIME content type string
     */
    String getContentType(String filename);
}
