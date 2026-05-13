package com.backend.nmcomputercare.contactForm.mailing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class ContactFormConfirmationEvent {


    private final String name,emailSentTo;
    private final String email;
    private final String phone;
    private final String service;
    private final String message;

    
}