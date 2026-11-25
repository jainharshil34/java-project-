package com.civicheat.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CommentResponse {
    private UUID id;
    private UUID complaintId;
    private String authorName;
    private String authorEmail;
    private String authorRole;
    private String content;
    private Boolean isOfficial;
    private Instant createdAt;
    private Instant updatedAt;
}
