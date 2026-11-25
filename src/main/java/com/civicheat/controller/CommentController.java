package com.civicheat.controller;

import com.civicheat.dto.request.AddCommentRequest;
import com.civicheat.dto.response.CommentResponse;
import com.civicheat.dto.response.PageResponse;
import com.civicheat.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/complaints/{complaintId}/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Discussion threads on civic complaints")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "Add a comment to a complaint")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID complaintId,
            @RequestBody @Valid AddCommentRequest request,
            Principal principal) {
        String email = principal != null ? principal.getName() : "anonymous@civicheat.local";
        CommentResponse response = commentService.addComment(complaintId, request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get comments for a complaint")
    public ResponseEntity<PageResponse<CommentResponse>> getComments(
            @PathVariable UUID complaintId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<CommentResponse> response = commentService.getComments(
                complaintId, PageRequest.of(page, size, Sort.by("createdAt").ascending()));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Soft-delete a comment (author or admin only)")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID complaintId,
            @PathVariable UUID commentId,
            Principal principal) {
        String email = principal != null ? principal.getName() : "anonymous@civicheat.local";
        commentService.deleteComment(commentId, email);
        return ResponseEntity.noContent().build();
    }
}
