package com.backend.nmcomputercare.user.dtos;
import lombok.Builder; import lombok.Data;
import java.util.List;
@Data @Builder
public class PagedUserResponse {
    private List<UserResponse> content;
    private int page, size, totalPages;
    private long totalItems;
    private boolean first, last;
}
