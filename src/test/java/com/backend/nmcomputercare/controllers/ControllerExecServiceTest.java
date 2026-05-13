package com.backend.nmcomputercare.controllers;

import com.backend.nmcomputercare.contactForm.controller.ContactFormController;
import com.backend.nmcomputercare.contactForm.dtos.ContactFormRequest;
import com.backend.nmcomputercare.contactForm.dtos.ContactFormResponse;
import com.backend.nmcomputercare.contactForm.service.ContactFormService;
import com.backend.nmcomputercare.newsletter.controller.NewsletterController;
import com.backend.nmcomputercare.newsletter.dtos.NewsletterRequest;
import com.backend.nmcomputercare.newsletter.dtos.NewsletterResponse;
import com.backend.nmcomputercare.newsletter.service.NewsletterService;
import com.backend.nmcomputercare.subscribe.controller.SubscriptionController;
import com.backend.nmcomputercare.subscribe.controller.VerificationController;
import com.backend.nmcomputercare.subscribe.dtos.SubscribeRequest;
import com.backend.nmcomputercare.subscribe.dtos.SubscriptionResponse;
import com.backend.nmcomputercare.subscribe.dtos.VerificationRequest;
import com.backend.nmcomputercare.subscribe.dtos.VerificationResponse;
import com.backend.nmcomputercare.subscribe.service.SubscriptionService;
import com.backend.nmcomputercare.subscribe.service.VerificationService;
import com.backend.nmcomputercare.utils.ExceptionAdvice;
import com.backend.nmcomputercare.utils.RequestContract;
import com.backend.nmcomputercare.utils.ResponseContract;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControllerExecServiceTest {

    @Mock private ContactFormService contactFormService;
    @Mock private SubscriptionService subscriptionService;
    @Mock private NewsletterService newsletterService;
    @Mock private VerificationService verificationService;

    private ExecutorService executor;
    private ExceptionAdvice advice;

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        advice = new ExceptionAdvice();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void contactFormSubmitDispatchesThroughExecService() {
        ContactFormController controller = new ContactFormController(contactFormService, executor, advice);
        ContactFormRequest request = ContactFormRequest.builder()
                .name("Ada")
                .email("ada@example.com")
                .numbers("0812345678")
                .service("Repair")
                .message("Laptop repair please")
                .build();
        ContactFormResponse responseBody = ContactFormResponse.builder()
                .id(1L)
                .name("Ada")
                .email("ada@example.com")
                .numbers("0812345678")
                .service("Repair")
                .message("Laptop repair please")
                .sentDate(LocalDateTime.now())
                .status((byte) 0)
                .build();
        AtomicBoolean usedVirtualThread = new AtomicBoolean(false);
        when(contactFormService.callable(eq("sendCustomerRequest"), same(request))).thenAnswer(invocation -> {
            usedVirtualThread.set(Thread.currentThread().isVirtual());
            return responseList(responseBody);
        });

        var response = controller.submit(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertSingleBodyItem(response.getBody(), responseBody);
        assertThat(usedVirtualThread).isTrue();
        verify(contactFormService).callable("sendCustomerRequest", request);
    }

    @Test
    void subscriptionSubscribeDispatchesThroughExecService() {
        SubscriptionController controller = new SubscriptionController(subscriptionService, executor, advice);
        SubscribeRequest request = SubscribeRequest.builder().email("subscriber@example.com").build();
        SubscriptionResponse responseBody = SubscriptionResponse.builder()
                .id(4L)
                .email("subscriber@example.com")
                .active(true)
                .status((byte) 0)
                .build();
        doReturn(responseList(responseBody))
                .when(subscriptionService).callable(eq("subscribe"), same(request));

        var response = controller.subscribe(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertSingleBodyItem(response.getBody(), responseBody);
        verify(subscriptionService).callable("subscribe", request);
    }

    @Test
    void newsletterCreateDispatchesThroughExecService() {
        NewsletterController controller = new NewsletterController(newsletterService, executor, advice);
        NewsletterRequest request = NewsletterRequest.builder()
                .title("Security Tips")
                .content("A long enough newsletter body for customers.")
                .build();
        NewsletterResponse responseBody = NewsletterResponse.builder()
                .id(8L)
                .title("Security Tips")
                .content("A long enough newsletter body for customers.")
                .status((byte) 0)
                .build();
        doReturn(responseList(responseBody))
                .when(newsletterService).callable(eq("createNewsletter"), same(request));

        var response = controller.createNewsletter(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertSingleBodyItem(response.getBody(), responseBody);
        verify(newsletterService).callable("createNewsletter", request);
    }

    @Test
    void verificationRedirectsAfterExecServiceVerification() {
        VerificationController controller = new VerificationController(verificationService, executor, advice);
        ReflectionTestUtils.setField(controller, "frontendBaseUrl", "https://front.example/");
        VerificationResponse responseBody = VerificationResponse.builder()
                .id(9L)
                .name("Ada Lovelace")
                .email("ada@example.com")
                .active(true)
                .status((byte) 1)
                .build();
        doReturn(responseList(responseBody))
                .when(verificationService).callable(eq("verifySubscription"), any(VerificationRequest.class));

        var response = controller.verify("token-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .hasToString("https://front.example/subscription/confirmed?name=Ada+Lovelace");

        ArgumentCaptor<RequestContract> requestCaptor = ArgumentCaptor.forClass(RequestContract.class);
        verify(verificationService).callable(eq("verifySubscription"), requestCaptor.capture());
        assertThat(requestCaptor.getValue())
                .isInstanceOfSatisfying(VerificationRequest.class,
                        request -> assertThat(request.getToken()).isEqualTo("token-123"));
    }

    private static List<? extends ResponseContract> responseList(ResponseContract response) {
        return List.of(response);
    }

    private static void assertSingleBodyItem(List<? extends ResponseContract> body,
                                             ResponseContract expected) {
        assertThat(body).isNotNull();
        assertThat(body).hasSize(1);
        assertThat(body.get(0)).isSameAs(expected);
    }
}
