package com.backend.nmcomputercare.basiccare.controller;

import com.backend.nmcomputercare.basiccare.dtos.*;
import com.backend.nmcomputercare.basiccare.entity.BasicCareStatus;
import com.backend.nmcomputercare.basiccare.service.BasicCarePlanService;
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
 * REST controller for all Basic Care Plan operations.
 * Base path: {@code /api/v1/basic-care-plans}
 *
 * <h3>Endpoints</h3>
 * <pre>
 *  POST    /api/v1/basic-care-plans                    → create
 *  PUT     /api/v1/basic-care-plans                    → update (partial patch via body)
 *  DELETE  /api/v1/basic-care-plans/{id}               → delete
 *  GET     /api/v1/basic-care-plans                    → findAll (paginated)
 *  GET     /api/v1/basic-care-plans/{id}               → findById
 *  GET     /api/v1/basic-care-plans/status/{status}    → findByStatus (paginated)
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/basic-care-plans")
public class BasicCarePlanController extends ExecService {

    private static final Logger logger = LoggerFactory.getLogger(BasicCarePlanController.class);

    private final BasicCarePlanService service;

    public BasicCarePlanController(BasicCarePlanService service,
                                   ExecutorService controllerExecutorService,
                                   ExceptionAdvice advice) {
        super(controllerExecutorService, advice);
        this.service = service;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  POST  /api/v1/basic-care-plans
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Create a new Basic Care Plan.
     *
     * <p>The quote total is calculated automatically from the selected
     * components.  Initial status is always {@code CREATED (0)}.
     *
     * @return {@code 201 Created} with the persisted record
     */
    @PostMapping
    public ResponseEntity<List<? extends ResponseContract>> create(
            @RequestBody CreateBasicCarePlanRequest request) {

        logger.info("POST /basic-care-plans | os={} drivers={} software={}",
                request.getOperationSystem(),
                request.getUpgradeDrivers(),
                request.getAdditionalPerformanceSoftware());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(exec(service, "createBasicCarePlan", request));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PUT  /api/v1/basic-care-plans
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Partially update an existing Basic Care Plan.
     *
     * <p>Only the fields provided in the request body are updated; omitted
     * fields retain their current values.  Include {@code "status"} to drive
     * lifecycle transitions (e.g. mark as PAID after payment confirmation).
     *
     * @return {@code 200 OK} with the updated record
     */
    @PutMapping
    public ResponseEntity<List<? extends ResponseContract>> update(
            @RequestBody UpdateBasicCarePlanRequest request) {

        logger.info("PUT /basic-care-plans | id={}", request.getId());

        return ResponseEntity.ok(exec(service, "updateBasicCarePlan", request));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DELETE  /api/v1/basic-care-plans/{id}
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Hard-delete a Basic Care Plan record by ID.
     *
     * <p>Returns a snapshot of the deleted record for client-side audit logging.
     *
     * @return {@code 200 OK} with the deleted record snapshot
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<List<? extends ResponseContract>> delete(
            @PathVariable Long id) {

        logger.info("DELETE /basic-care-plans/{}", id);

        return ResponseEntity.ok(exec(service, "deleteBasicCarePlan",
                DeleteBasicCarePlanRequest.builder().id(id).build()));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET  /api/v1/basic-care-plans
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Retrieve all Basic Care Plans with pagination and sorting.
     *
     * <p>Query parameters:
     * <ul>
     *   <li>{@code page}      — zero-based page index (default: 0)</li>
     *   <li>{@code size}      — records per page, max 100 (default: 20)</li>
     *   <li>{@code sortBy}    — field to sort by (default: {@code createdDate})</li>
     *   <li>{@code direction} — {@code ASC} or {@code DESC} (default: {@code DESC})</li>
     * </ul>
     *
     * <p>Example: {@code GET /api/v1/basic-care-plans?page=0&size=10&sortBy=totalQuote&direction=ASC}
     */
    @GetMapping
    public ResponseEntity<PagedBasicCarePlanResponse> findAll(
            @RequestParam(defaultValue = "0")           int            page,
            @RequestParam(defaultValue = "20")          int            size,
            @RequestParam(defaultValue = "createdDate") String         sortBy,
            @RequestParam(defaultValue = "DESC")        Sort.Direction direction) {

        logger.info("GET /basic-care-plans | page={} size={} sortBy={} direction={}",
                page, size, sortBy, direction);

        FindAllBasicCarePlansRequest request = FindAllBasicCarePlansRequest.builder()
                .page(page).size(size).sortBy(sortBy).direction(direction)
                .build();

        @SuppressWarnings("unchecked")
        List<BasicCarePlanResponse> content =
                (List<BasicCarePlanResponse>) exec(service, "findAllBasicCarePlans", request);

        return ResponseEntity.ok(buildPagedResponse(content, page, size));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET  /api/v1/basic-care-plans/{id}
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Retrieve a single Basic Care Plan by its primary key.
     *
     * <p>Example: {@code GET /api/v1/basic-care-plans/42}
     */
    @GetMapping("/{id}")
    public ResponseEntity<List<? extends ResponseContract>> findById(
            @PathVariable Long id) {

        logger.info("GET /basic-care-plans/{}", id);

        return ResponseEntity.ok(exec(service, "findBasicCarePlanById",
                FindBasicCarePlanByIdRequest.builder().id(id).build()));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET  /api/v1/basic-care-plans/status/{status}
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Retrieve plans filtered by lifecycle status code, with pagination.
     *
     * <p>Status codes:
     * <pre>
     *  0 = CREATED
     *  1 = MISSING_PAYMENT
     *  2 = QUOTE_REJECTED
     *  3 = PAID
     *  4 = WAITING_APPOINTMENT
     *  5 = COMPLETED
     * </pre>
     *
     * <p>Example: {@code GET /api/v1/basic-care-plans/status/3?page=0&size=10}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<PagedBasicCarePlanResponse> findByStatus(
            @PathVariable                              byte           status,
            @RequestParam(defaultValue = "0")          int            page,
            @RequestParam(defaultValue = "20")         int            size,
            @RequestParam(defaultValue = "createdDate") String         sortBy,
            @RequestParam(defaultValue = "DESC")       Sort.Direction direction) {

        logger.info("GET /basic-care-plans/status/{} | page={} size={}", status, page, size);

        // Validate the status code early so the controller can return a 400
        // before the service layer is invoked.
        BasicCareStatus.fromCode(status);

        FindBasicCarePlansByStatusRequest request =
                FindBasicCarePlansByStatusRequest.builder()
                        .status(status)
                        .page(page).size(size).sortBy(sortBy).direction(direction)
                        .build();

        @SuppressWarnings("unchecked")
        List<BasicCarePlanResponse> content =
                (List<BasicCarePlanResponse>) exec(service, "findBasicCarePlansByStatus", request);

        return ResponseEntity.ok(buildPagedResponse(content, page, size));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Builds a {@link PagedBasicCarePlanResponse} envelope from a content list.
     * Mirrors the pagination logic in {@code SubscriptionController}.
     */
    private PagedBasicCarePlanResponse buildPagedResponse(
            List<BasicCarePlanResponse> content, int page, int size) {

        int  safeSize   = (size > 0 && size <= 100) ? size : 20;
        long totalItems = content.size();
        int  totalPages = safeSize > 0 ? (int) Math.ceil((double) totalItems / safeSize) : 1;

        return PagedBasicCarePlanResponse.builder()
                .content(content)
                .page(page)
                .size(safeSize)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .build();
    }
}
