package com.backend.nmcomputercare.validators;

import com.backend.nmcomputercare.contactForm.dtos.ContactFormRequest;
import com.backend.nmcomputercare.contactForm.exceptions.EmptyFieldException;
import com.backend.nmcomputercare.contactForm.validator.ContactFormValidator;
import com.backend.nmcomputercare.newsletter.dtos.NewsletterRequest;
import com.backend.nmcomputercare.newsletter.validaator.NewsletterValidator;
import com.backend.nmcomputercare.subscribe.dtos.SubscribeRequest;
import com.backend.nmcomputercare.subscribe.validaator.SubscriptionValidator;
import com.backend.nmcomputercare.utils.ExceptionAdvice;
import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidatorBehaviorTest {

    private final ExceptionAdvice advice = new ExceptionAdvice();

    @Test
    void contactFormValidatorAcceptsCompleteRequest() {
        ContactFormValidator validator = new ContactFormValidator(advice);

        assertThatCode(() -> validator.validate(ContactFormRequest.builder()
                .name("Ada")
                .email("ada@example.com")
                .numbers("0812345678")
                .service("Repair")
                .message("Please repair my laptop.")
                .build()))
                .doesNotThrowAnyException();
    }

    @Test
    void contactFormValidatorRejectsMissingAndOversizedFields() {
        ContactFormValidator validator = new ContactFormValidator(advice);

        assertThatThrownBy(() -> validator.validate(ContactFormRequest.builder()
                .email("ada@example.com")
                .numbers("0812345678")
                .service("Repair")
                .message("Please repair my laptop.")
                .build()))
                .isInstanceOf(EmptyFieldException.class)
                .hasMessageContaining("Name is empty");

        assertThatThrownBy(() -> validator.validate(ContactFormRequest.builder()
                .name("Ada")
                .email("ada@example.com")
                .numbers("0812345678")
                .service("Repair")
                .message("x".repeat(301))
                .build()))
                .isInstanceOf(EmptyFieldException.class)
                .hasMessageContaining("300 characters");
    }

    @Test
    void subscriptionValidatorRejectsEmailWithoutDomainDot() {
        SubscriptionValidator validator = new SubscriptionValidator(advice);

        assertThatCode(() -> validator.validate(SubscribeRequest.builder()
                .email("subscriber@example.com")
                .build()))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> validator.validate(SubscribeRequest.builder()
                .email("subscriber@example")
                .build()))
                .isInstanceOf(IncorrectRequestSentException.class)
                .hasMessageContaining("invalid");
    }

    @Test
    void newsletterValidatorRejectsThinContent() {
        NewsletterValidator validator = new NewsletterValidator(advice);

        assertThatCode(() -> validator.validate(NewsletterRequest.builder()
                .title("Security Tips")
                .content("Use updates, backups, and strong passwords every week.")
                .build()))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> validator.validate(NewsletterRequest.builder()
                .title("Hi")
                .content("Use updates, backups, and strong passwords every week.")
                .build()))
                .isInstanceOf(IncorrectRequestSentException.class)
                .hasMessageContaining("at least 3 characters");

        assertThatThrownBy(() -> validator.validate(NewsletterRequest.builder()
                .title("Security Tips")
                .content("Too short")
                .build()))
                .isInstanceOf(IncorrectRequestSentException.class)
                .hasMessageContaining("at least 20 characters");
    }
}
