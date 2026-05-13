package com.backend.nmcomputercare.subscribe.dtos;

import com.backend.nmcomputercare.utils.ResponseContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponse implements ResponseContract {
    private Long id;
    private String name;
    private String email;
    private boolean active;
    private byte status;
}
