package com.backend.nmcomputercare.subscribe.dtos;

import com.backend.nmcomputercare.utils.PageableRequest;
import com.backend.nmcomputercare.utils.RequestContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Pagination + sort request for the findAllSubscriptions endpoint.
 * Mirrors {@code FindAllContactFormsRequest} exactly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindAllSubscriptionsRequest implements PageableRequest, RequestContract {

    @Builder.Default private int            page      = 0;
    @Builder.Default private int            size      = 20;
    @Builder.Default private String         sortBy    = "subscribedDate";
    @Builder.Default private Sort.Direction direction = Sort.Direction.DESC;

    @Override
    public Pageable toPageable() {
        int safeSize = (size > 0 && size <= 100) ? size : 20;
        return PageRequest.of(page, safeSize, Sort.by(direction, sortBy));
    }
}
