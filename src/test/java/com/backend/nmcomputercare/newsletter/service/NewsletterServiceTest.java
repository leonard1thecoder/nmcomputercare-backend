package com.backend.nmcomputercare.newsletter.service;

import com.backend.nmcomputercare.newsletter.dtos.FindNewsletterByIdRequest;
import com.backend.nmcomputercare.newsletter.dtos.NewsletterRequest;
import com.backend.nmcomputercare.newsletter.dtos.NewsletterResponse;
import com.backend.nmcomputercare.newsletter.entity.Newsletter;
import com.backend.nmcomputercare.newsletter.repository.NewsletterRepository;
import com.backend.nmcomputercare.newsletter.validaator.NewsletterValidator;
import com.backend.nmcomputercare.utils.ExceptionAdvice;
import com.backend.nmcomputercare.utils.RedisService;
import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import com.backend.nmcomputercare.utils.exceptions.ServiceNameNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsletterServiceTest {

    @Mock private NewsletterRepository repository;
    @Mock private RedisService redisService;
    @Mock private NewsletterValidator validator;

    @Test
    void createNewsletterSavesDraftAndMapsResponse() {
        NewsletterService service = service();
        NewsletterRequest request = NewsletterRequest.builder()
                .title("Security Tips")
                .content("Use strong passwords and keep your devices updated.")
                .build();
        when(repository.save(any(Newsletter.class))).thenAnswer(invocation -> {
            Newsletter newsletter = invocation.getArgument(0);
            newsletter.setId(77L);
            return newsletter;
        });

        var response = service.createNewsletter(request);

        assertThat(response).singleElement().isInstanceOfSatisfying(NewsletterResponse.class, item -> {
            assertThat(item.getId()).isEqualTo(77L);
            assertThat(item.getTitle()).isEqualTo("Security Tips");
            assertThat(item.getStatus()).isEqualTo((byte) 0);
            assertThat(item.getCreatedDate()).isNotNull();
        });
        verify(validator).validate(request);
    }

    @Test
    void findNewsletterByIdReturnsMappedResponse() {
        NewsletterService service = service();
        Newsletter newsletter = Newsletter.builder()
                .id(9L)
                .title("Maintenance")
                .content("Keep dust out of your devices and patch frequently.")
                .createdDate(LocalDateTime.now())
                .status((byte) 1)
                .build();
        when(repository.findById(9L)).thenReturn(Optional.of(newsletter));

        var response = service.callable(
                "findNewsletterById",
                FindNewsletterByIdRequest.builder().id(9L).build());

        assertThat(response).singleElement().isInstanceOfSatisfying(NewsletterResponse.class, item -> {
            assertThat(item.getId()).isEqualTo(9L);
            assertThat(item.getTitle()).isEqualTo("Maintenance");
            assertThat(item.getStatus()).isEqualTo((byte) 1);
        });
    }

    @Test
    void findNewsletterByIdThrowsWhenMissing() {
        NewsletterService service = service();
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.callable(
                "findNewsletterById",
                FindNewsletterByIdRequest.builder().id(404L).build()))
                .isInstanceOf(IncorrectRequestSentException.class)
                .hasMessageContaining("No newsletter found");
    }

    @Test
    void unknownServiceNameThrows() {
        NewsletterService service = service();

        assertThatThrownBy(() -> service.callable("missing", NewsletterRequest.builder().build()))
                .isInstanceOf(ServiceNameNotFoundException.class)
                .hasMessageContaining("Service name not found");
    }

    private NewsletterService service() {
        return new NewsletterService(
                repository,
                redisService,
                new ExceptionAdvice(),
                validator);
    }
}
