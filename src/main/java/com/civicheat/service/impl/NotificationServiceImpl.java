package com.civicheat.service.impl;

import com.civicheat.dto.response.NotificationResponse;
import com.civicheat.dto.response.PageResponse;
import com.civicheat.entity.Notification;
import com.civicheat.entity.User;
import com.civicheat.repository.NotificationRepository;
import com.civicheat.repository.UserRepository;
import com.civicheat.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(String userEmail, Pageable pageable) {
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) return PageResponse.empty();
        Page<Notification> page = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(userOpt.get().getId(), pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .map(u -> notificationRepository.countByRecipientIdAndIsReadFalse(u.getId()))
                .orElse(0L);
    }

    @Override
    @Transactional
    public int markAllRead(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .map(u -> notificationRepository.markAllReadForUser(u.getId()))
                .orElse(0);
    }

    @Override
    @Transactional
    public void sendNotification(String recipientEmail, String title, String message,
                                  UUID complaintId, String type) {
        userRepository.findByEmail(recipientEmail).ifPresent(recipient -> {
            Notification notif = Notification.builder()
                    .recipient(recipient)
                    .title(title)
                    .message(message)
                    .complaintId(complaintId)
                    .notificationType(type)
                    .build();
            notificationRepository.save(notif);
            log.debug("Notification sent to {}: {}", recipientEmail, title);
        });
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .complaintId(n.getComplaintId())
                .isRead(n.getIsRead())
                .notificationType(n.getNotificationType())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
