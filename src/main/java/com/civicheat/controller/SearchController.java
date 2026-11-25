package com.civicheat.controller;

import com.civicheat.dto.response.ComplaintResponse;
import com.civicheat.dto.response.PageResponse;
import com.civicheat.mapper.ComplaintMapper;
import com.civicheat.repository.ComplaintRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Full-text keyword search across complaint titles and descriptions.
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Full-text keyword search over civic complaints")
public class SearchController {

    private final ComplaintRepository complaintRepository;
    private final ComplaintMapper complaintMapper;

    @GetMapping("/complaints")
    @Operation(summary = "Search complaints by keyword in title or description")
    public ResponseEntity<PageResponse<ComplaintResponse>> searchComplaints(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String keyword = q != null ? q.trim() : "";
        if (keyword.isEmpty()) {
            return ResponseEntity.ok(PageResponse.empty());
        }
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var resultPage = complaintRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword, pageable)
                .map(complaintMapper::toResponse);

        return ResponseEntity.ok(PageResponse.from(resultPage));
    }
}
