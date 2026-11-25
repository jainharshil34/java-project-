package com.civicheat.service.impl;

import com.civicheat.dto.request.AddCommentRequest;
import com.civicheat.dto.response.CommentResponse;
import com.civicheat.dto.response.PageResponse;
import com.civicheat.entity.Complaint;
import com.civicheat.entity.ComplaintComment;
import com.civicheat.entity.User;
import com.civicheat.enums.Role;
import com.civicheat.exception.ResourceNotFoundException;
import com.civicheat.repository.ComplaintCommentRepository;
import com.civicheat.repository.ComplaintRepository;
import com.civicheat.repository.UserRepository;
import com.civicheat.service.CommentService;
import com.civicheat.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final ComplaintCommentRepository commentRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public CommentResponse addComment(UUID complaintId, AddCommentRequest request, String userEmail) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found: " + complaintId));

        User author = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        boolean isOfficial = author.getRole() == Role.OFFICER || author.getRole() == Role.ADMIN;

        ComplaintComment comment = ComplaintComment.builder()
                .complaint(complaint)
                .author(author)
                .content(request.getContent())
                .isOfficial(isOfficial)
                .build();

        ComplaintComment saved = commentRepository.save(comment);

        // Notify the complaint reporter about the new comment (if different user)
        if (complaint.getReporter() != null &&
                !complaint.getReporter().getEmail().equals(userEmail)) {
            String notifTitle = isOfficial
                    ? "Official update on your complaint"
                    : "New comment on your complaint";
            String notifMsg = String.format("'%s': %s", complaint.getTitle(), request.getContent());
            notificationService.sendNotification(
                    complaint.getReporter().getEmail(),
                    notifTitle, notifMsg,
                    complaintId,
                    "NEW_COMMENT"
            );
        }

        log.info("Comment added to complaint {} by {} (official={})", complaintId, userEmail, isOfficial);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getComments(UUID complaintId, Pageable pageable) {
        if (!complaintRepository.existsById(complaintId)) {
            throw new ResourceNotFoundException("Complaint not found: " + complaintId);
        }
        Page<ComplaintComment> page = commentRepository
                .findByComplaintIdAndIsDeletedFalseOrderByCreatedAtAsc(complaintId, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId, String userEmail) {
        ComplaintComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));

        User requester = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        // Allow deletion only by author or admin
        boolean isAuthor = comment.getAuthor().getEmail().equals(userEmail);
        boolean isAdmin = requester.getRole() == Role.ADMIN;
        if (!isAuthor && !isAdmin) {
            throw new IllegalArgumentException("Not authorized to delete this comment");
        }

        comment.setIsDeleted(true);
        commentRepository.save(comment);
        log.info("Comment {} soft-deleted by {}", commentId, userEmail);
    }

    private CommentResponse toResponse(ComplaintComment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .complaintId(c.getComplaint().getId())
                .authorName(c.getAuthor().getFullName())
                .authorEmail(c.getAuthor().getEmail())
                .authorRole(c.getAuthor().getRole().name())
                .content(c.getContent())
                .isOfficial(c.getIsOfficial())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
