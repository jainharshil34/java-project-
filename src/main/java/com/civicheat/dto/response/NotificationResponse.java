package com.civicheat.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private String title;
    private String message;
    private UUID complaintId;
    private Boolean isRead;
    private String notificationType;
    private Instant createdAt;
}
