package com.backend.nmcomputercare.contactForm.mailing;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;

@Service
public class ContactEmailService {
    private static final Logger logger = LoggerFactory.getLogger(ContactEmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    // ── Change this to your admin inbox ──────────────────────────────────────
    private static final String ADMIN_EMAIL = "admin@nmcomputercare.co.za";

    public ContactEmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Sends the admin notification email when the contact form is submitted.
     *
     * @param name     Visitor's full name
     * @param email    Visitor's email address
     * @param phone    Visitor's phone number
     * @param service  Service they are enquiring about
     * @param message  Their message body
     */

    public void sendAdminNotification(String name,
                                      String email,
                                      String phone,
                                      String service,
                                      String message,
                                      String sentToEmail) throws MessagingException {

        // ── Build Thymeleaf context ───────────────────────────────────────────
        Context ctx = new Context();
        ctx.setVariable("name",        name);
        ctx.setVariable("email",       email);
        ctx.setVariable("phone",       phone);
        ctx.setVariable("service",     service);
        ctx.setVariable("message",     message);
        ctx.setVariable("submittedAt", LocalDateTime.now());

        // ── Render template → HTML string ────────────────────────────────────
        // Template file lives at:  src/main/resources/templates/contact-form-admin.html
        String htmlBody = templateEngine.process("contact-form-admin", ctx);

        // ── Build MIME message ────────────────────────────────────────────────
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

        helper.setTo(sentToEmail);
        helper.setSubject("📬 New Enquiry from " + name + " – NM Computer Care");
        helper.setText(htmlBody, true);   // true = HTML content

        // ── Send ─────────────────────────────────────────────────────────────
        mailSender.send(mime);
    }


}