package com.backend.nmcomputercare.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public interface PageableRequest {

    int getPage();
    int getSize();
    String getSortBy();
    Sort.Direction getDirection();

    default Pageable toPageable() {
        int safePage = Math.max(0, getPage());
        int safeSize = (getSize() > 0 && getSize() <= 100) ? getSize() : 20;

        if (getSortBy() != null && !getSortBy().isBlank()) {
            Sort sort = Sort.by(
                    getDirection() != null ? getDirection() : Sort.Direction.DESC,
                    getSortBy()
            );
            return PageRequest.of(safePage, safeSize, sort);
        }
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "sentDate"));
    }
}