package com.civicheat.service;

import com.civicheat.dto.request.AddCommentRequest;
import com.civicheat.dto.response.CommentResponse;
import com.civicheat.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CommentService {
    CommentResponse addComment(UUID complaintId, AddCommentRequest request, String userEmail);
    PageResponse<CommentResponse> getComments(UUID complaintId, Pageable pageable);
    void deleteComment(UUID commentId, String userEmail);
}
