package com.backend.nmcomputercare.subscribe.dtos;

import com.backend.nmcomputercare.utils.ResponseContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Read-only view of a persisted {@code Subscription}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse implements ResponseContract {
    private Long          id;
    private String        name;
    private String        email;
    private LocalDateTime subscribedDate;
    private LocalDateTime unsubscribedDate;
    private boolean       active;
    private byte          status;
}
