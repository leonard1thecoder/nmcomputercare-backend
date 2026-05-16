package com.backend.nmcomputercare.user.dtos;

import com.backend.nmcomputercare.user.entity.Privilege;
import com.backend.nmcomputercare.utils.RequestContract;
import lombok.Builder;
import lombok.Data;

/**
 * Payload for {@code POST /api/v1/auth/register}.
 *
 * <p>If {@code role} is null it defaults to {@link Privilege#KING_SPARKON_USER}.
 * Only an authenticated ADMIN may register another ADMIN.
 */
@Data
@Builder
public class RegisterRequest implements RequestContract {
    private String    name;
    private String    surname;
    private String    emailAddress;
    private String    password;
    /** Defaults to KING_SPARKON_USER when omitted. */
    private Privilege role;
}
