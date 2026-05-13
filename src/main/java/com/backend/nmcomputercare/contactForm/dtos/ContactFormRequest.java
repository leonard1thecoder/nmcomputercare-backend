package com.backend.nmcomputercare.contactForm.dtos;

import com.backend.nmcomputercare.utils.RequestContract;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ContactFormRequest implements RequestContract {

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;

    @JsonProperty("phone")
    private String numbers;

    @JsonProperty("service")
    private String service;

    @JsonProperty("message")
    private String message;

}