package com.backend.nmcomputercare.contactForm.dtos;

import com.backend.nmcomputercare.utils.RequestContract;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class FindContactFormByIdRequest implements RequestContract {
    @JsonProperty("form_id")
    private Long id;
}
