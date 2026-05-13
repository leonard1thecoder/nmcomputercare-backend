package com.backend.nmcomputercare.subscribe.service;

import com.backend.nmcomputercare.subscribe.dtos.FindSubscriptionByEmailRequest;
import com.backend.nmcomputercare.subscribe.dtos.SubscribeRequest;
import com.backend.nmcomputercare.subscribe.dtos.SubscriptionResponse;
import com.backend.nmcomputercare.subscribe.dtos.UnsubscribeRequest;
import com.backend.nmcomputercare.subscribe.entity.Subscription;
import com.backend.nmcomputercare.subscribe.repository.SubscriptionRepository;
import com.backend.nmcomputercare.subscribe.validaator.SubscriptionValidator;
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
class SubscriptionServiceTest {

    @Mock private SubscriptionRepository repository;
    @Mock private RedisService redisService;
    @Mock private SubscriptionValidator validator;

    @Test
    void subscribeCreatesPendingActiveSubscription() {
        SubscriptionService service = service();
        SubscribeRequest request = SubscribeRequest.builder().email("subscriber@example.com").build();
        when(repository.findByEmail("subscriber@example.com")).thenReturn(Optional.empty());
        when(repository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription saved = invocation.getArgument(0);
            saved.setId(44L);
            return saved;
        });

        var response = service.subscribe(request);

        assertThat(response).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(44L);
            assertThat(item.getEmail()).isEqualTo("subscriber@example.com");
            assertThat(item.isActive()).isTrue();
            assertThat(item.getStatus()).isEqualTo((byte) 0);
            assertThat(item.getSubscribedDate()).isNotNull();
        });
        verify(validator).validate(request);
    }

    @Test
    void subscribeReactivatesInactiveSubscription() {
        SubscriptionService service = service();
        Subscription existing = Subscription.builder()
                .id(5L)
                .email("back@example.com")
                .subscribedDate(LocalDateTime.now().minusDays(5))
                .unsubscribedDate(LocalDateTime.now().minusDays(1))
                .active(false)
                .status((byte) 2)
                .build();
        when(repository.findByEmail("back@example.com")).thenReturn(Optional.of(existing));
        when(repository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.subscribe(SubscribeRequest.builder().email("back@example.com").build());

        assertThat(response).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(5L);
            assertThat(item.isActive()).isTrue();
            assertThat(item.getStatus()).isEqualTo((byte) 0);
            assertThat(item.getUnsubscribedDate()).isNull();
        });
        assertThat(existing.isActive()).isTrue();
        assertThat(existing.getUnsubscribedDate()).isNull();
    }

    @Test
    void subscribeRejectsAlreadyActiveAddress() {
        SubscriptionService service = service();
        Subscription existing = Subscription.builder()
                .email("taken@example.com")
                .active(true)
                .status((byte) 1)
                .build();
        when(repository.findByEmail("taken@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.subscribe(
                SubscribeRequest.builder().email("taken@example.com").build()))
                .isInstanceOf(IncorrectRequestSentException.class)
                .hasMessageContaining("already subscribed");
    }

    @Test
    void unsubscribeSoftDeletesActiveSubscription() {
        SubscriptionService service = service();
        Subscription existing = Subscription.builder()
                .id(6L)
                .email("leave@example.com")
                .subscribedDate(LocalDateTime.now().minusDays(2))
                .active(true)
                .status((byte) 1)
                .build();
        when(repository.findByEmail("leave@example.com")).thenReturn(Optional.of(existing));
        when(repository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.unsubscribe(UnsubscribeRequest.builder().email("leave@example.com").build());

        assertThat(response).singleElement().isInstanceOfSatisfying(SubscriptionResponse.class, item -> {
            assertThat(item.isActive()).isFalse();
            assertThat(item.getStatus()).isEqualTo((byte) 2);
            assertThat(item.getUnsubscribedDate()).isNotNull();
        });
        assertThat(existing.isActive()).isFalse();
    }

    @Test
    void findSubscriptionByEmailThrowsWhenMissing() {
        SubscriptionService service = service();
        when(repository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.callable(
                "findSubscriptionByEmail",
                FindSubscriptionByEmailRequest.builder().email("missing@example.com").build()))
                .isInstanceOf(IncorrectRequestSentException.class)
                .hasMessageContaining("No subscription found");
    }

    @Test
    void unknownServiceNameThrows() {
        SubscriptionService service = service();

        assertThatThrownBy(() -> service.callable("missing", SubscribeRequest.builder().build()))
                .isInstanceOf(ServiceNameNotFoundException.class)
                .hasMessageContaining("Service name not found");
    }

    private SubscriptionService service() {
        return new SubscriptionService(repository, redisService, validator);
    }
}
