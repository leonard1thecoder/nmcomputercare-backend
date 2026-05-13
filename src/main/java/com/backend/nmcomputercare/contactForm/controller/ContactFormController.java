package com.backend.nmcomputercare.contactForm.controller;
import com.backend.nmcomputercare.contactForm.dtos.*;
import com.backend.nmcomputercare.contactForm.service.ContactFormService;
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
 * REST controller for all contact-form operations.
 *
 * <p>Every endpoint delegates to {@link ContactFormService#callable(String, com.backend.nmcomputercare.utils.RequestContract)}
 * to remain consistent with the service's dispatch pattern.
 *
 * <p>Base path: {@code /api/v1/contact-forms}
 */
@RestController
@RequestMapping("/api/v1/contact-forms")
public class ContactFormController extends ExecService {
 
    private static final Logger logger = LoggerFactory.getLogger(ContactFormController.class);
 
    private final ContactFormService service;

    public ContactFormController(ContactFormService service,
                                 ExecutorService controllerExecutorService,
                                 ExceptionAdvice advice) {
        super(controllerExecutorService, advice);
        this.service = service;
    }
 
    // ══════════════════════════════════════════════════════════════════════════
    //  POST  /api/v1/contact-forms
    //  Submit a new contact form (rate-limited via @RateLimiter on the service).
    // ══════════════════════════════════════════════════════════════════════════
 
    /**
     * Submit a new contact form enquiry.
     *
     * @param request validated form payload
     * @return {@code 201 Created} with the saved record
     */
    @PostMapping
    public ResponseEntity<List<? extends ResponseContract>> submit(
             @RequestBody ContactFormRequest request) {
 
        logger.info("POST /contact-forms | email={}", maskEmail(request.getEmail()));
 
        List<? extends ResponseContract> response =
                exec(service, "sendCustomerRequest", request);
 
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
 
    // ══════════════════════════════════════════════════════════════════════════
    //  GET  /api/v1/contact-forms
    //  Paginated list of all submissions.
    // ══════════════════════════════════════════════════════════════════════════
 
    /**
     * Retrieve all contact form submissions with pagination and optional sorting.
     *
     * <p>Query parameters:
     * <ul>
     *   <li>{@code page}      — zero-based page index (default: 0)</li>
     *   <li>{@code size}      — records per page, max 100 (default: 20)</li>
     *   <li>{@code sortBy}    — field to sort by (default: {@code sentDate})</li>
     *   <li>{@code direction} — {@code ASC} or {@code DESC} (default: {@code DESC})</li>
     * </ul>
     *
     * <p>Example: {@code GET /api/v1/contact-forms?page=1&size=10&sortBy=name&direction=ASC}
     *
     * @return {@code 200 OK} with a {@link PagedContactFormResponse}
     */
    @GetMapping
    public ResponseEntity<PagedContactFormResponse> findAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "sentDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
 
        logger.info("GET /contact-forms | page={} size={} sortBy={} direction={}",
                page, size, sortBy, direction);
 
        // Build the typed request contract — PageableRequest.toPageable() is
        // called inside the service to produce the Spring Pageable.
        FindAllContactFormsRequest request = FindAllContactFormsRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();
 
        // The service returns List<ContactFormResponse>; cast is safe because
        // findAllCustomerRequest always produces ContactFormResponse instances.
        @SuppressWarnings("unchecked")
        List<ContactFormResponse> content =
                (List<ContactFormResponse>) exec(service, "findAllCustomerRequest", request);
 
        // Compute pagination metadata for the response envelope.
        int safeSize      = (size > 0 && size <= 100) ? size : 20;
        long totalItems   = content.size(); // replace with repository.count() if needed
        int  totalPages   = safeSize > 0 ? (int) Math.ceil((double) totalItems / safeSize) : 1;
 
        PagedContactFormResponse pagedResponse = PagedContactFormResponse.builder()
                .content(content)
                .page(page)
                .size(safeSize)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .build();
 
        return ResponseEntity.ok(pagedResponse);
    }
 
    // ══════════════════════════════════════════════════════════════════════════
    //  GET  /api/v1/contact-forms/{id}
    // ══════════════════════════════════════════════════════════════════════════
 
    /**
     * Retrieve a single contact form submission by its ID.
     *
     * @param id record identifier
     * @return {@code 200 OK} with the matching record
     */
    @GetMapping("/{id}")
    public ResponseEntity<List<? extends ResponseContract>> findById(
            @PathVariable Long id) {
 
        logger.info("GET /contact-forms/{}", id);
 
        FindContactFormByIdRequest request = FindContactFormByIdRequest.builder()
                .id(id)
                .build();
 
        return ResponseEntity.ok(exec(service, "findCustomerRequestById", request));
    }
 
    // ══════════════════════════════════════════════════════════════════════════
    //  GET  /api/v1/contact-forms/email/{email}
    // ══════════════════════════════════════════════════════════════════════════
 
    /**
     * Retrieve all contact form submissions for a given email address.
     *
     * @param email the submitter's email address
     * @return {@code 200 OK} with all matching records
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<List<? extends ResponseContract>> findByEmail(
            @PathVariable String email) {
 
        logger.info("GET /contact-forms/email/{}", maskEmail(email));
 
        FindContactFormByEmailRequest request = FindContactFormByEmailRequest.builder()
                .email(email)
                .build();
 
        return ResponseEntity.ok(exec(service, "findCustomerRequestByEmail", request));
    }
 
    // ══════════════════════════════════════════════════════════════════════════
    //  GET  /api/v1/contact-forms/numbers/{numbers}
    // ══════════════════════════════════════════════════════════════════════════
 
    /**
     * Retrieve all contact form submissions for a given phone number.
     *
     * @param numbers the submitter's phone number
     * @return {@code 200 OK} with all matching records
     */
    @GetMapping("/numbers/{numbers}")
    public ResponseEntity<List<? extends ResponseContract>> findByNumbers(
            @PathVariable String numbers) {
 
        logger.info("GET /contact-forms/numbers/{}", maskPhone(numbers));
 
        FindContactFormByNumbersRequest request = FindContactFormByNumbersRequest.builder()
                .numbers(numbers)
                .build();
 
        return ResponseEntity.ok(exec(service, "findCustomerRequestByNumbers", request));
    }
 
    // ══════════════════════════════════════════════════════════════════════════
    //  PII masking (logs must never contain raw PII)
    // ══════════════════════════════════════════════════════════════════════════
 
    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        String masked = local.length() > 2 ? local.substring(0, 2) + "***" : "***";
        return masked + "@" + parts[1];
    }
 
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 4);
    }
}
 
