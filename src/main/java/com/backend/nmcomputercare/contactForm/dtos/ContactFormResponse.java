package com.backend.nmcomputercare.contactForm.dtos;

import com.backend.nmcomputercare.utils.ResponseContract;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
public class ContactFormResponse implements ResponseContract {

    @JsonProperty("form_id")
    private Long id;
    @JsonProperty("sender_name")
    private String name;
    @JsonProperty("senser_email_address")

    private String email;
    @JsonProperty("sender_numbers")

    private String numbers;
    @JsonProperty("sender_requested_service")

    private String service;
    @JsonProperty("sender_message")

    private String message;
    @JsonProperty("message_sent_date")

    private LocalDateTime sentDate;

    @JsonProperty("is_message_responded")
    private Byte status;
}
