package com.backend.nmcomputercare.subscribe.dtos;

import com.backend.nmcomputercare.utils.RequestContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ─────────────────────────────────────────────────────────────────────────────
// Subscribe
// ─────────────────────────────────────────────────────────────────────────────


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscribeRequest implements RequestContract {

    private String email;
}
