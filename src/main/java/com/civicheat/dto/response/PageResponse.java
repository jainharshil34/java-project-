package com.civicheat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standard paginated response wrapper for collections.
 *
 * @param <T> Element type contained in page
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Paginated response container")
public class PageResponse<T> {

    @Schema(description = "List of items in the current page")
    private List<T> content;

    @Schema(description = "Current page number (0-indexed)", example = "0")
    private int pageNumber;

    @Schema(description = "Size of each page", example = "20")
    private int pageSize;

    @Schema(description = "Total number of elements across all pages", example = "105")
    private long totalElements;

    @Schema(description = "Total number of pages available", example = "6")
    private int totalPages;

    @Schema(description = "Whether this is the last page", example = "false")
    private boolean isLast;

    @Schema(description = "Whether this is the first page", example = "true")
    private boolean isFirst;

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .isFirst(page.isFirst())
                .build();
    }

    public static <T> PageResponse<T> empty() {
        return PageResponse.<T>builder()
                .content(java.util.List.of())
                .pageNumber(0)
                .pageSize(0)
                .totalElements(0)
                .totalPages(0)
                .isLast(true)
                .isFirst(true)
                .build();
    }
}
