package com.backend.nmcomputercare.contactForm.dtos;

import com.backend.nmcomputercare.utils.RequestContract;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@Builder
@Data
@AllArgsConstructor
public class FindContactFormByNumbersRequest implements RequestContract {
    @JsonProperty("sender_numbers")
    private String numbers;
}
