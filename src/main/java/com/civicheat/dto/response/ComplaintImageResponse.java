package com.civicheat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Response representation of an attached complaint image.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Media image attachment metadata")
public class ComplaintImageResponse {

    private UUID id;
    private String imageUrl;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private Instant createdAt;
}
