package com.civicheat.service;

import com.civicheat.dto.response.NotificationResponse;
import com.civicheat.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    PageResponse<NotificationResponse> getMyNotifications(String userEmail, Pageable pageable);
    long countUnread(String userEmail);
    int markAllRead(String userEmail);
    void sendNotification(String recipientEmail, String title, String message, java.util.UUID complaintId, String type);
}
