package com.backend.nmcomputercare.user.dtos;

import com.backend.nmcomputercare.utils.ResponseContract;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Returned by both {@code /auth/register} and {@code /auth/login}.
 *
 * <p>The {@code token} is a signed JWT.  Include it in subsequent requests as:
 * <pre>Authorization: Bearer &lt;token&gt;</pre>
 */
@Data
@Builder
public class AuthResponse implements ResponseContract {
    private String token;
    /** Always {@code "Bearer"}. */
    private String        tokenType;
    private Long          userId;
    private String        name;
    private String        surname;
    private String        emailAddress;
    private String        role;
    private String        roleDisplayName;
    private String        status;
    private String        statusDisplayName;
    private LocalDateTime registeredDate;
}
