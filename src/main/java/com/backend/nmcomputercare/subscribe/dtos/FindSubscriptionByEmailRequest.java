package com.backend.nmcomputercare.subscribe.dtos;

import com.backend.nmcomputercare.utils.RequestContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Carries an email address for a subscription lookup.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindSubscriptionByEmailRequest implements RequestContract {
    private String email;
}
