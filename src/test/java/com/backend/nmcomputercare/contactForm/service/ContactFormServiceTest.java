package com.backend.nmcomputercare.contactForm.service;

import com.backend.nmcomputercare.contactForm.dtos.ContactFormRequest;
import com.backend.nmcomputercare.contactForm.dtos.ContactFormResponse;
import com.backend.nmcomputercare.contactForm.dtos.FindContactFormByIdRequest;
import com.backend.nmcomputercare.contactForm.entity.ContactForm;
import com.backend.nmcomputercare.contactForm.repository.ContactFormRepository;
import com.backend.nmcomputercare.contactForm.validator.ContactFormValidator;
import com.backend.nmcomputercare.utils.ExceptionAdvice;
import com.backend.nmcomputercare.utils.RedisService;
import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import com.backend.nmcomputercare.utils.exceptions.ServiceNameNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactFormServiceTest {

    @Mock private ApplicationEventPublisher publisher;
    @Mock private RedisService redisService;
    @Mock private ContactFormRepository repository;
    @Mock private ContactFormValidator validator;

    @Test
    void sendCustomerRequestValidatesPublishesEmailsAndSaves() {
        ContactFormService service = service();
        ContactFormRequest request = ContactFormRequest.builder()
                .name("Ada")
                .email("ada@example.com")
                .numbers("0812345678")
                .service("Laptop Repair")
                .message("My laptop needs attention.")
                .build();
        ContactForm saved = ContactForm.builder()
                .id(12L)
                .name(request.getName())
                .email(request.getEmail())
                .numbers(request.getNumbers())
                .service(request.getService())
                .message(request.getMessage())
                .sentDate(LocalDateTime.now())
                .status((byte) 0)
                .build();
        when(repository.save(any(ContactForm.class))).thenReturn(saved);

        List<ContactFormResponse> response = service.sendCustomerRequest(request);

        assertThat(response).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(12L);
            assertThat(item.getEmail()).isEqualTo("ada@example.com");
            assertThat(item.getService()).isEqualTo("Laptop Repair");
        });
        verify(validator).validate(request);
        verify(publisher, times(3)).publishEvent(any(Object.class));

        ArgumentCaptor<ContactForm> entityCaptor = ArgumentCaptor.forClass(ContactForm.class);
        verify(repository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue()).satisfies(entity -> {
            assertThat(entity.getEmail()).isEqualTo("ada@example.com");
            assertThat(entity.getStatus()).isZero();
            assertThat(entity.getSentDate()).isNotNull();
        });
    }

    @Test
    void findCustomerRequestByIdReturnsMappedResponse() {
        ContactFormService service = service();
        ContactForm form = ContactForm.builder()
                .id(3L)
                .name("Grace")
                .email("grace@example.com")
                .numbers("0820000000")
                .service("Diagnostics")
                .message("Please inspect my PC.")
                .sentDate(LocalDateTime.now())
                .status((byte) 1)
                .build();
        when(repository.findById(3L)).thenReturn(Optional.of(form));

        var response = service.callable(
                "findCustomerRequestById",
                FindContactFormByIdRequest.builder().id(3L).build());

        assertThat(response).singleElement().isInstanceOfSatisfying(ContactFormResponse.class, item -> {
            assertThat(item.getId()).isEqualTo(3L);
            assertThat(item.getName()).isEqualTo("Grace");
            assertThat(item.getStatus()).isEqualTo((byte) 1);
        });
    }

    @Test
    void findCustomerRequestByIdThrowsWhenMissing() {
        ContactFormService service = service();
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.callable(
                "findCustomerRequestById",
                FindContactFormByIdRequest.builder().id(404L).build()))
                .isInstanceOf(IncorrectRequestSentException.class)
                .hasMessageContaining("No contact form found");
    }

    @Test
    void unknownServiceNameThrows() {
        ContactFormService service = service();

        assertThatThrownBy(() -> service.callable("missing", ContactFormRequest.builder().build()))
                .isInstanceOf(ServiceNameNotFoundException.class)
                .hasMessageContaining("Service name not found");
    }

    private ContactFormService service() {
        return new ContactFormService(
                publisher,
                redisService,
                repository,
                new ExceptionAdvice(),
                validator);
    }
}
