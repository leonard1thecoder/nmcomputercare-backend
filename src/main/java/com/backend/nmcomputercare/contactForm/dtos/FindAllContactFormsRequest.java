package com.backend.nmcomputercare.contactForm.dtos;

import com.backend.nmcomputercare.utils.PageableRequest;
import com.backend.nmcomputercare.utils.RequestContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

/**
 * Request contract for paginated retrieval of all contact forms.
 *
 * <p>Passed to {@code ContactFormService.callable("findAllCustomerRequest", request)}.
 * The service calls {@link #toPageable()} (inherited from {@link PageableRequest})
 * to build a Spring {@link org.springframework.data.domain.Pageable}.
 *
 * <p>All fields are optional — the {@link PageableRequest} default implementation
 * applies safe bounds (page 0, size 20, sorted by {@code sentDate DESC}) when
 * values are omitted or invalid.
 *
 * <p>Example JSON body:
 * <pre>{@code
 * {
 *   "page": 0,
 *   "size": 15,
 *   "sortBy": "sentDate",
 *   "direction": "DESC"
 * }
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindAllContactFormsRequest implements RequestContract, PageableRequest {

    /**
     * Zero-based page number. Defaults to {@code 0} when null.
     */
    @Builder.Default
    private int page = 0;

    /**
     * Number of records per page. Capped at 100 by {@link PageableRequest#toPageable()}.
     * Defaults to {@code 20} when null or invalid.
     */
    @Builder.Default
    private int size = 20;

    /**
     * Entity field name to sort by (e.g. {@code "sentDate"}, {@code "name"}).
     * Defaults to {@code "sentDate"} when null or blank.
     */
    @Builder.Default
    private String sortBy = "sentDate";

    /**
     * Sort direction. Accepts {@code "ASC"} or {@code "DESC"}.
     * Defaults to {@link Sort.Direction#DESC} when null.
     */
    @Builder.Default
    private Sort.Direction direction = Sort.Direction.DESC;
}