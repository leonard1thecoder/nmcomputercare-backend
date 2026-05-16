package com.backend.nmcomputercare.user.dtos;
import com.backend.nmcomputercare.utils.ResponseContract;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime;
/** Read-only user record returned by admin endpoints. */
@Data @Builder
public class UserResponse implements ResponseContract {
    private Long id;
    private String name, surname, emailAddress;
    private String role, roleDisplayName;
    private String status, statusDisplayName;
    private LocalDateTime registeredDate;
}
