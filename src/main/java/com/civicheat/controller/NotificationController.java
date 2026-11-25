package com.civicheat.controller;

import com.civicheat.dto.response.NotificationResponse;
import com.civicheat.dto.response.PageResponse;
import com.civicheat.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification management")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get my notifications (paginated)")
    public ResponseEntity<PageResponse<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        String email = principal != null ? principal.getName() : "anonymous@civicheat.local";
        return ResponseEntity.ok(notificationService.getMyNotifications(email, PageRequest.of(page, size)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count unread notifications")
    public ResponseEntity<Map<String, Long>> unreadCount(Principal principal) {
        String email = principal != null ? principal.getName() : "anonymous@civicheat.local";
        long count = notificationService.countUnread(email);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PostMapping("/mark-all-read")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Map<String, Integer>> markAllRead(Principal principal) {
        String email = principal != null ? principal.getName() : "anonymous@civicheat.local";
        int updated = notificationService.markAllRead(email);
        return ResponseEntity.ok(Map.of("markedRead", updated));
    }
}
