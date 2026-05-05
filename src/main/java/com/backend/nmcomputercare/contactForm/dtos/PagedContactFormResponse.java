package com.backend.nmcomputercare.contactForm.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Wraps a page of {@link ContactFormResponse} records with pagination metadata
 * so callers know the total record count and how to request the next page.
 *
 * <p>Example JSON response:
 * <pre>{@code
 * {
 *   "content":    [ { ... }, { ... } ],
 *   "page":       0,
 *   "size":       20,
 *   "totalItems": 47,
 *   "totalPages": 3,
 *   "first":      true,
 *   "last":       false
 * }
 * }</pre>
 */
@Data
@Builder
public class PagedContactFormResponse {

    /** The records for the current page. */
    private List<ContactFormResponse> content;

    /** Current zero-based page number. */
    private int page;

    /** Number of records requested per page. */
    private int size;

    /** Total number of records across all pages. */
    private long totalItems;

    /** Total number of pages available. */
    private int totalPages;

    /** {@code true} if this is the first page. */
    private boolean first;

    /** {@code true} if this is the last page. */
    private boolean last;
}