package com.backend.nmcomputercare.user.dtos;
import com.backend.nmcomputercare.user.entity.UserStatus;
import com.backend.nmcomputercare.utils.RequestContract;
import lombok.Builder; import lombok.Data;
/** Admin payload to change a user's verification status. */
@Data @Builder
public class UpdateUserStatusRequest implements RequestContract {
    private Long id;
    private UserStatus status;
}
