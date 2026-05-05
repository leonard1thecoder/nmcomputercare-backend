package com.backend.nmcomputercare.contactForm.mailing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class ContactFormEmailEvent {
    private String
            email,
            name,
            emailSentTo,
            phone,
            service,
            message,
    emailType;


}
