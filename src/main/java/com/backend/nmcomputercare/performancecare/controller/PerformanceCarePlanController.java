package com.backend.nmcomputercare.performancecare.controller;

import com.backend.nmcomputercare.performancecare.dtos.*;
import com.backend.nmcomputercare.performancecare.entity.PerformanceCareStatus;
import com.backend.nmcomputercare.performancecare.service.PerformanceCarePlanService;
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
 * REST controller for all Performance Care Plan operations.
 * Base path: {@code /api/v1/performance-care-plans}
 *
 * <pre>
 *  POST    /api/v1/performance-care-plans               → create (201)
 *  PUT     /api/v1/performance-care-plans               → partial update (200)
 *  DELETE  /api/v1/performance-care-plans/{id}          → hard delete (200 + snapshot)
 *  GET     /api/v1/performance-care-plans               → findAll (paginated)
 *  GET     /api/v1/performance-care-plans/{id}          → findById
 *  GET     /api/v1/performance-care-plans/status/{code} → findByStatus (paginated)
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/performance-care-plans")
public class PerformanceCarePlanController extends ExecService {

    private static final Logger logger =
            LoggerFactory.getLogger(PerformanceCarePlanController.class);

    private final PerformanceCarePlanService service;

    public PerformanceCarePlanController(PerformanceCarePlanService service,
                                         ExecutorService controllerExecutorService,
                                         ExceptionAdvice advice) {
        super(controllerExecutorService, advice);
        this.service = service;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  POST  /api/v1/performance-care-plans
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Create a new Performance Care Plan.
     *
     * <p>Quote is auto-calculated: only the highest-priority upgrade price
     * (CPU → RAM → GPU) counts, plus the device labour premium.
     *
     * @return {@code 201 Created} with the persisted record and full quote breakdown
     */
    @PostMapping
    public ResponseEntity<List<? extends ResponseContract>> create(
            @RequestBody CreatePerformanceCarePlanRequest request) {

        logger.info("POST /performance-care-plans | cpu={} ram={} gpu={} device={}",
                request.getCpuUpdate(), request.getRamUpdate(),
                request.getUpgradeGraphicCard(), request.getDeviceType());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(exec(service, "createPerformanceCarePlan", request));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PUT  /api/v1/performance-care-plans
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Partially update an existing Performance Care Plan.
     *
     * <p>Only non-null fields in the request body are applied.
     * Include {@code "status"} to drive lifecycle transitions.
     *
     * @return {@code 200 OK} with the updated record
     */
    @PutMapping
    public ResponseEntity<List<? extends ResponseContract>> update(
            @RequestBody UpdatePerformanceCarePlanRequest request) {

        logger.info("PUT /performance-care-plans | id={}", request.getId());
        return ResponseEntity.ok(exec(service, "updatePerformanceCarePlan", request));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DELETE  /api/v1/performance-care-plans/{id}
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Hard-delete a Performance Care Plan by ID.
     * Returns a snapshot of the deleted record for client-side audit logging.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<List<? extends ResponseContract>> delete(
            @PathVariable Long id) {

        logger.info("DELETE /performance-care-plans/{}", id);
        return ResponseEntity.ok(exec(service, "deletePerformanceCarePlan",
                DeletePerformanceCarePlanRequest.builder().id(id).build()));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET  /api/v1/performance-care-plans
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Retrieve all Performance Care Plans with pagination and sorting.
     *
     * <p>Example: {@code GET /api/v1/performance-care-plans?page=0&size=10&sortBy=totalQuote&direction=ASC}
     */
    @GetMapping
    public ResponseEntity<PagedPerformanceCarePlanResponse> findAll(
            @RequestParam(defaultValue = "0")           int            page,
            @RequestParam(defaultValue = "20")          int            size,
            @RequestParam(defaultValue = "createdDate") String         sortBy,
            @RequestParam(defaultValue = "DESC")        Sort.Direction direction) {

        logger.info("GET /performance-care-plans | page={} size={}", page, size);

        FindAllPerformanceCarePlansRequest request =
                FindAllPerformanceCarePlansRequest.builder()
                        .page(page).size(size).sortBy(sortBy).direction(direction)
                        .build();

        @SuppressWarnings("unchecked")
        List<PerformanceCarePlanResponse> content =
                (List<PerformanceCarePlanResponse>) exec(service, "findAllPerformanceCarePlans", request);

        return ResponseEntity.ok(buildPagedResponse(content, page, size));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET  /api/v1/performance-care-plans/{id}
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Retrieve a single Performance Care Plan by primary key.
     *
     * <p>Example: {@code GET /api/v1/performance-care-plans/7}
     */
    @GetMapping("/{id}")
    public ResponseEntity<List<? extends ResponseContract>> findById(
            @PathVariable Long id) {

        logger.info("GET /performance-care-plans/{}", id);
        return ResponseEntity.ok(exec(service, "findPerformanceCarePlanById",
                FindPerformanceCarePlanByIdRequest.builder().id(id).build()));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET  /api/v1/performance-care-plans/status/{status}
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Retrieve plans filtered by lifecycle status code, with pagination.
     *
     * <pre>
     *  0 = CREATED             3 = PAID
     *  1 = MISSING_PAYMENT     4 = WAITING_APPOINTMENT
     *  2 = QUOTE_REJECTED      5 = COMPLETED
     * </pre>
     *
     * <p>Example: {@code GET /api/v1/performance-care-plans/status/4?page=0&size=10}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<PagedPerformanceCarePlanResponse> findByStatus(
            @PathVariable                              byte           status,
            @RequestParam(defaultValue = "0")          int            page,
            @RequestParam(defaultValue = "20")         int            size,
            @RequestParam(defaultValue = "createdDate") String         sortBy,
            @RequestParam(defaultValue = "DESC")       Sort.Direction direction) {

        logger.info("GET /performance-care-plans/status/{} | page={} size={}", status, page, size);

        // Validate early so the controller returns 400 before reaching the service.
        PerformanceCareStatus.fromCode(status);

        FindPerformanceCarePlansByStatusRequest request =
                FindPerformanceCarePlansByStatusRequest.builder()
                        .status(status).page(page).size(size)
                        .sortBy(sortBy).direction(direction)
                        .build();

        @SuppressWarnings("unchecked")
        List<PerformanceCarePlanResponse> content =
                (List<PerformanceCarePlanResponse>) exec(service, "findPerformanceCarePlansByStatus", request);

        return ResponseEntity.ok(buildPagedResponse(content, page, size));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Helper
    // ══════════════════════════════════════════════════════════════════════

    private PagedPerformanceCarePlanResponse buildPagedResponse(
            List<PerformanceCarePlanResponse> content, int page, int size) {

        int  safeSize   = (size > 0 && size <= 100) ? size : 20;
        long totalItems = content.size();
        int  totalPages = safeSize > 0 ? (int) Math.ceil((double) totalItems / safeSize) : 1;

        return PagedPerformanceCarePlanResponse.builder()
                .content(content)
                .page(page).size(safeSize)
                .totalItems(totalItems).totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .build();
    }
}
