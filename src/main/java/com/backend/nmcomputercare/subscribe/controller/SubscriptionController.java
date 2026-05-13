package com.backend.nmcomputercare.subscribe.controller;


import com.backend.nmcomputercare.subscribe.dtos.*;
import com.backend.nmcomputercare.subscribe.service.SubscriptionService;
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
 * REST controller for all subscription operations.
 * Base path: {@code /api/v1/subscriptions}
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController extends ExecService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionService service;

    public SubscriptionController(SubscriptionService service,
                                  ExecutorService controllerExecutorService,
                                  ExceptionAdvice advice) {
        super(controllerExecutorService, advice);
        this.service = service;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  POST  /api/v1/subscriptions
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Opt an email address into the newsletter.
     * Re-activates a previously unsubscribed address transparently.
     *
     * @return {@code 201 Created} with the saved record
     */
    @PostMapping
    public ResponseEntity<List<? extends ResponseContract>> subscribe(
             @RequestBody SubscribeRequest request) {

        logger.info("POST /subscriptions | email={}", maskEmail(request.getEmail()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(exec(service, "subscribe", request));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DELETE  /api/v1/subscriptions
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Soft-delete a subscription (active = false).
     * Email is passed in the request body to avoid logging PII in proxy access logs.
     */
    @DeleteMapping
    public ResponseEntity<List<? extends ResponseContract>> unsubscribe(
             @RequestBody UnsubscribeRequest request) {

        logger.info("DELETE /subscriptions | email={}", maskEmail(request.getEmail()));
        return ResponseEntity.ok(exec(service, "unsubscribe", request));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GET  /api/v1/subscriptions
    //  Paginated list — mirrors ContactFormController.findAll exactly.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Retrieve all subscriptions (active and inactive) with pagination and sorting.
     *
     * <p>Query parameters:
     * <ul>
     *   <li>{@code page}      — zero-based page index (default: 0)</li>
     *   <li>{@code size}      — records per page, max 100 (default: 20)</li>
     *   <li>{@code sortBy}    — field to sort by (default: {@code subscribedDate})</li>
     *   <li>{@code direction} — {@code ASC} or {@code DESC} (default: {@code DESC})</li>
     * </ul>
     *
     * <p>Example: {@code GET /api/v1/subscriptions?page=0&size=10&sortBy=email&direction=ASC}
     */
    @GetMapping
    public ResponseEntity<PagedSubscriptionResponse> findAll(
            @RequestParam(defaultValue = "0")                int            page,
            @RequestParam(defaultValue = "20")               int            size,
            @RequestParam(defaultValue = "subscribedDate")   String         sortBy,
            @RequestParam(defaultValue = "DESC")             Sort.Direction direction) {

        logger.info("GET /subscriptions | page={} size={} sortBy={} direction={}",
                page, size, sortBy, direction);

        FindAllSubscriptionsRequest request = FindAllSubscriptionsRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        @SuppressWarnings("unchecked")
        List<SubscriptionResponse> content =
                (List<SubscriptionResponse>) exec(service, "findAllSubscriptions", request);

        int  safeSize   = (size > 0 && size <= 100) ? size : 20;
        long totalItems = content.size();
        int  totalPages = safeSize > 0 ? (int) Math.ceil((double) totalItems / safeSize) : 1;

        return ResponseEntity.ok(PagedSubscriptionResponse.builder()
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
    //  GET  /api/v1/subscriptions/active
    //  Active-only paginated list — same envelope, separate cache key.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Retrieve only active subscribers with pagination and sorting.
     *
     * <p>Example: {@code GET /api/v1/subscriptions/active?page=0&size=10}
     */
    @GetMapping("/active")
    public ResponseEntity<PagedSubscriptionResponse> findActive(
            @RequestParam(defaultValue = "0")                int            page,
            @RequestParam(defaultValue = "20")               int            size,
            @RequestParam(defaultValue = "subscribedDate")   String         sortBy,
            @RequestParam(defaultValue = "DESC")             Sort.Direction direction) {

        logger.info("GET /subscriptions/active | page={} size={} sortBy={} direction={}",
                page, size, sortBy, direction);

        FindAllSubscriptionsRequest request = FindAllSubscriptionsRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        @SuppressWarnings("unchecked")
        List<SubscriptionResponse> content =
                (List<SubscriptionResponse>) exec(service, "findActiveSubscriptions", request);

        int  safeSize   = (size > 0 && size <= 100) ? size : 20;
        long totalItems = content.size();
        int  totalPages = safeSize > 0 ? (int) Math.ceil((double) totalItems / safeSize) : 1;

        return ResponseEntity.ok(PagedSubscriptionResponse.builder()
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
    //  GET  /api/v1/subscriptions/email/{email}
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Retrieve the subscription record for a given email address.
     *
     * <p>Note: URL-encode the {@code @} symbol, e.g.
     * {@code GET /api/v1/subscriptions/email/john%40example.com}
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<List<? extends ResponseContract>> findByEmail(
            @PathVariable String email) {

        logger.info("GET /subscriptions/email/{}", maskEmail(email));
        return ResponseEntity.ok(exec(service, "findSubscriptionByEmail",
                FindSubscriptionByEmailRequest.builder().email(email).build()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PII masking
    // ══════════════════════════════════════════════════════════════════════════

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local  = parts[0];
        String masked = local.length() > 2 ? local.substring(0, 2) + "***" : "***";
        return masked + "@" + parts[1];
    }
}
