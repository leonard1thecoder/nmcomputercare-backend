package com.backend.nmcomputercare.businesscare.dtos;
import lombok.Builder; import lombok.Data;
import java.util.List;
@Data @Builder
public class PagedBusinessCarePlanResponse {
    private List<BusinessCarePlanResponse> content;
    private int page; private int size;
    private long totalItems; private int totalPages;
    private boolean first; private boolean last;
}
