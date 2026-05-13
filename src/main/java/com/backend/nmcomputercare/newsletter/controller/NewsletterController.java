package com.backend.nmcomputercare.newsletter.controller;

import com.backend.nmcomputercare.newsletter.dtos.*;
import com.backend.nmcomputercare.newsletter.service.NewsletterService;
import com.backend.nmcomputercare.utils.ExceptionAdvice;
import com.backend.nmcomputercare.utils.ExecService;
import com.backend.nmcomputercare.utils.ResponseContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * REST controller for all newsletter operations.
 * Base path: {@code /api/v1/newsletters}
 */
@RestController
@RequestMapping("/api/v1/newsletters")
public class NewsletterController extends ExecService {

    private static final Logger logger = LoggerFactory.getLogger(NewsletterController.class);

    private final NewsletterService service;

    public NewsletterController(NewsletterService service,
                                ExecutorService controllerExecutorService,
                                ExceptionAdvice advice) {
        super(controllerExecutorService, advice);
        this.service = service;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  POST  /api/v1/newsletters
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Create a new newsletter draft.
     *
     * @return {@code 201 Created} with the saved record
     */
    @PostMapping
    public ResponseEntity<List<? extends ResponseContract>> createNewsletter(
             @RequestBody NewsletterRequest request) {

        logger.info("POST /newsletters | title={} ", request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(exec(service, "createNewsletter", request));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GET  /api/v1/newsletters
    //  Paginated list — mirrors ContactFormController.findAll exactly.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Retrieve all newsletters with pagination and optional sorting.
     *
     * <p>Query parameters:
     * <ul>
     *   <li>{@code page}      — zero-based page index (default: 0)</li>
     *   <li>{@code size}      — records per page, max 100 (default: 20)</li>
     *   <li>{@code sortBy}    — field to sort by (default: {@code createdDate})</li>
     *   <li>{@code direction} — {@code ASC} or {@code DESC} (default: {@code DESC})</li>
     * </ul>
     *
     * <p>Example: {@code GET /api/v1/newsletters?page=0&size=10&sortBy=title&direction=ASC}
     */
    @GetMapping
    public ResponseEntity<PagedNewsletterResponse> findAll(
            @RequestParam(defaultValue = "0")           int            page,
            @RequestParam(defaultValue = "20")          int            size,
            @RequestParam(defaultValue = "createdDate") String         sortBy,
            @RequestParam(defaultValue = "DESC")        Sort.Direction direction) {

        logger.info("GET /newsletters | page={} size={} sortBy={} direction={}",
                page, size, sortBy, direction);

        FindAllNewslettersRequest request = FindAllNewslettersRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        @SuppressWarnings("unchecked")
        List<NewsletterResponse> content =
                (List<NewsletterResponse>) exec(service, "findAllNewsletters", request);

        int  safeSize   = (size > 0 && size <= 100) ? size : 20;
        long totalItems = content.size();
        int  totalPages = safeSize > 0 ? (int) Math.ceil((double) totalItems / safeSize) : 1;

        return ResponseEntity.ok(PagedNewsletterResponse.builder()
                .content(content)
                .page(page)
                .size(safeSize)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .build());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GET  /api/v1/newsletters/{id}
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/{id}")
    public ResponseEntity<List<? extends ResponseContract>> findById(
            @PathVariable Long id) {

        logger.info("GET /newsletters/{}", id);
        return ResponseEntity.ok(exec(service, "findNewsletterById",
                FindNewsletterByIdRequest.builder().id(id).build()));
    }

}
