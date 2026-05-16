package com.backend.nmcomputercare.user.dtos;

import com.backend.nmcomputercare.utils.RequestContract;
import lombok.Builder;
import lombok.Data;

/** Payload for {@code POST /api/v1/auth/login}. */
@Data
@Builder
public class LoginRequest implements RequestContract {
    private String emailAddress;
    private String password;
}
