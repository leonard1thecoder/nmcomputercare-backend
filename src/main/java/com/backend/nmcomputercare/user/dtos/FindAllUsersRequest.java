package com.backend.nmcomputercare.user.dtos;

import com.backend.nmcomputercare.utils.PageableRequest;
import com.backend.nmcomputercare.utils.RequestContract;
import lombok.*;
import org.springframework.data.domain.Sort;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindAllUsersRequest implements RequestContract, PageableRequest {

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
