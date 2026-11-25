package com.civicheat.controller;

import com.civicheat.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public REST Controller exposing endpoints to fetch stored civic complaint media attachments.
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "Media Files", description = "Endpoints for retrieving uploaded complaint attachments and media")
public class FileController {

    private final FileStorageService fileStorageService;

    @GetMapping("/{filename:.+}")
    @Operation(summary = "Retrieve uploaded image attachment", description = "Serves stored images by filename with appropriate MIME content type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image successfully retrieved"),
            @ApiResponse(responseCode = "404", description = "Requested image not found"),
            @ApiResponse(responseCode = "400", description = "Invalid filename format")
    })
    public ResponseEntity<Resource> getFile(
            @Parameter(description = "Unique filename of the stored image", required = true)
            @PathVariable String filename) {
        Resource file = fileStorageService.loadFileAsResource(filename);
        String contentType = fileStorageService.getContentType(filename);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }
}
