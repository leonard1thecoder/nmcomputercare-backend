package com.backend.nmcomputercare.businesscare.controller;

import com.backend.nmcomputercare.businesscare.dtos.*;
import com.backend.nmcomputercare.businesscare.entity.BusinessCareStatus;
import com.backend.nmcomputercare.businesscare.service.BusinessCarePlanService;
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
 * REST controller for Business Care Plan operations.
 * Base path: {@code /api/v1/business-care-plans}
 *
 * <pre>
 *  POST    /api/v1/business-care-plans                → create (201)
 *  PUT     /api/v1/business-care-plans                → partial update (200)
 *  DELETE  /api/v1/business-care-plans/{id}           → hard delete (200 + snapshot)
 *  GET     /api/v1/business-care-plans                → findAll (paginated)
 *  GET     /api/v1/business-care-plans/{id}           → findById
 *  GET     /api/v1/business-care-plans/status/{code}  → findByStatus (paginated)
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/business-care-plans")
public class BusinessCarePlanController extends ExecService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessCarePlanController.class);

    private final BusinessCarePlanService service;

    public BusinessCarePlanController(BusinessCarePlanService service,
                                      ExecutorService controllerExecutorService,
                                      ExceptionAdvice advice) {
        super(controllerExecutorService, advice);
        this.service = service;
    }

    // ── POST ─────────────────────────────────────────────────────────────────

    /**
     * Create a new Business Care Plan.
     *
     * <p>Set {@code bulkType} to {@code PERFORMANCE_CARE} or {@code BASIC_CARE}.
     * The inactive section's fields must carry their NONE defaults — the
     * validator enforces this strictly.  Quote = unitPrice × quantity.
     */
    @PostMapping
    public ResponseEntity<List<? extends ResponseContract>> create(
            @RequestBody CreateBusinessCarePlanRequest request) {
        logger.info("POST /business-care-plans | bulkType={} qty={}",
                request.getBulkType(), request.getQuantity());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(exec(service, "createBusinessCarePlan", request));
    }

    // ── PUT ──────────────────────────────────────────────────────────────────

    /** Partially update a Business Care Plan. Only non-null fields are applied. */
    @PutMapping
    public ResponseEntity<List<? extends ResponseContract>> update(
            @RequestBody UpdateBusinessCarePlanRequest request) {
        logger.info("PUT /business-care-plans | id={}", request.getId());
        return ResponseEntity.ok(exec(service, "updateBusinessCarePlan", request));
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    /** Hard-delete a Business Care Plan. Returns a snapshot of the deleted record. */
    @DeleteMapping("/{id}")
    public ResponseEntity<List<? extends ResponseContract>> delete(@PathVariable Long id) {
        logger.info("DELETE /business-care-plans/{}", id);
        return ResponseEntity.ok(exec(service, "deleteBusinessCarePlan",
                DeleteBusinessCarePlanRequest.builder().id(id).build()));
    }

    // ── GET all ──────────────────────────────────────────────────────────────

    /**
     * Paginated list of all Business Care Plans.
     * Example: {@code GET /api/v1/business-care-plans?page=0&size=10&sortBy=totalQuote&direction=ASC}
     */
    @GetMapping
    public ResponseEntity<PagedBusinessCarePlanResponse> findAll(
            @RequestParam(defaultValue = "0")           int            page,
            @RequestParam(defaultValue = "20")          int            size,
            @RequestParam(defaultValue = "createdDate") String         sortBy,
            @RequestParam(defaultValue = "DESC")        Sort.Direction direction) {

        logger.info("GET /business-care-plans | page={} size={}", page, size);
        FindAllBusinessCarePlansRequest request = FindAllBusinessCarePlansRequest.builder()
                .page(page).size(size).sortBy(sortBy).direction(direction).build();

        @SuppressWarnings("unchecked")
        List<BusinessCarePlanResponse> content =
                (List<BusinessCarePlanResponse>) exec(service, "findAllBusinessCarePlans", request);
        return ResponseEntity.ok(buildPagedResponse(content, page, size));
    }

    // ── GET by id ────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<List<? extends ResponseContract>> findById(@PathVariable Long id) {
        logger.info("GET /business-care-plans/{}", id);
        return ResponseEntity.ok(exec(service, "findBusinessCarePlanById",
                FindBusinessCarePlanByIdRequest.builder().id(id).build()));
    }

    // ── GET by status ────────────────────────────────────────────────────────

    /**
     * Paginated list filtered by lifecycle status.
     * <pre>
     *  0=CREATED  1=MISSING_PAYMENT  2=QUOTE_REJECTED
     *  3=PAID     4=WAITING_APPOINTMENT  5=COMPLETED
     * </pre>
     * Example: {@code GET /api/v1/business-care-plans/status/3?page=0&size=10}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<PagedBusinessCarePlanResponse> findByStatus(
            @PathVariable                              byte           status,
            @RequestParam(defaultValue = "0")          int            page,
            @RequestParam(defaultValue = "20")         int            size,
            @RequestParam(defaultValue = "createdDate") String        sortBy,
            @RequestParam(defaultValue = "DESC")       Sort.Direction direction) {

        logger.info("GET /business-care-plans/status/{} | page={} size={}", status, page, size);
        BusinessCareStatus.fromCode(status); // early 400 on bad code

        FindBusinessCarePlansByStatusRequest request =
                FindBusinessCarePlansByStatusRequest.builder()
                        .status(status).page(page).size(size)
                        .sortBy(sortBy).direction(direction).build();

        @SuppressWarnings("unchecked")
        List<BusinessCarePlanResponse> content =
                (List<BusinessCarePlanResponse>) exec(service, "findBusinessCarePlansByStatus", request);
        return ResponseEntity.ok(buildPagedResponse(content, page, size));
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private PagedBusinessCarePlanResponse buildPagedResponse(
            List<BusinessCarePlanResponse> content, int page, int size) {
        int  safeSize   = (size > 0 && size <= 100) ? size : 20;
        long totalItems = content.size();
        int  totalPages = safeSize > 0 ? (int) Math.ceil((double) totalItems / safeSize) : 1;
        return PagedBusinessCarePlanResponse.builder()
                .content(content).page(page).size(safeSize)
                .totalItems(totalItems).totalPages(totalPages)
                .first(page == 0).last(page >= totalPages - 1)
                .build();
    }
}
