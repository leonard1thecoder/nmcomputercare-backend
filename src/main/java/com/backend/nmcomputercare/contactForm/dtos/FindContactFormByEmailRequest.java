package com.backend.nmcomputercare.contactForm.dtos;

import com.backend.nmcomputercare.utils.RequestContract;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class FindContactFormByEmailRequest implements RequestContract {
    @JsonProperty("senser_email_address")
    private String email;

}
