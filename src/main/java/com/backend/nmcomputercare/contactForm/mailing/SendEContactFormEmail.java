package com.backend.nmcomputercare.contactForm.mailing;

import com.backend.nmcomputercare.contactForm.mailing.dto.ContactFormEmailEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class SendEContactFormEmail {

    private final  ContactEmailService mailService;

    @Async
    @EventListener
    public void sendMail(ContactFormEmailEvent event) {
        try {
            mailService.sendAdminNotification( event.getName(), event.getEmail(), event.getPhone(), event.getService(), event.getMessage(), event.getEmailSentTo());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
