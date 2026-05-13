package com.backend.nmcomputercare.subscribe.service;

import com.backend.nmcomputercare.subscribe.dtos.VerificationRequest;
import com.backend.nmcomputercare.subscribe.dtos.VerificationResponse;
import com.backend.nmcomputercare.subscribe.entity.Subscription;
import com.backend.nmcomputercare.subscribe.entity.VerificationToken;
import com.backend.nmcomputercare.subscribe.exceptions.TokenAlreadyUsedException;
import com.backend.nmcomputercare.subscribe.exceptions.TokenExpiredException;
import com.backend.nmcomputercare.subscribe.exceptions.TokenNotFoundException;
import com.backend.nmcomputercare.subscribe.repository.SubscriptionRepository;
import com.backend.nmcomputercare.subscribe.repository.VerificationTokenRepository;
import com.backend.nmcomputercare.utils.ExceptionAdvice;
import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import com.backend.nmcomputercare.utils.exceptions.ServiceNameNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock private VerificationTokenRepository tokenRepository;
    @Mock private SubscriptionRepository subscriptionRepository;

    @Test
    void createTokenForDeletesOldTokensAndSavesFreshToken() {
        VerificationService service = service();
        Subscription subscription = Subscription.builder()
                .id(2L)
                .email("verify@example.com")
                .build();
        when(tokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String rawToken = service.createTokenFor(subscription);

        assertThat(rawToken).isNotBlank();
        verify(tokenRepository).deleteBySubscriptionId(2L);

        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue()).satisfies(token -> {
            assertThat(token.getToken()).isEqualTo(rawToken);
            assertThat(token.getSubscription()).isSameAs(subscription);
            assertThat(token.isUsed()).isFalse();
            assertThat(token.getExpiresAt()).isAfter(token.getCreatedAt());
        });
    }

    @Test
    void verifyConfirmsSubscriptionAndConsumesToken() {
        VerificationService service = service();
        Subscription subscription = Subscription.builder()
                .id(3L)
                .name("Ada")
                .email("ada@example.com")
                .active(false)
                .status((byte) 0)
                .build();
        VerificationToken token = VerificationToken.builder()
                .token("token-123")
                .subscription(subscription)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();
        when(tokenRepository.findByToken("token-123")).thenReturn(Optional.of(token));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Subscription confirmed = service.verify("token-123");

        assertThat(confirmed).isSameAs(subscription);
        assertThat(subscription.isActive()).isTrue();
        assertThat(subscription.getStatus()).isEqualTo((byte) 1);
        assertThat(token.isUsed()).isTrue();
        verify(subscriptionRepository).save(subscription);
        verify(tokenRepository).save(token);
    }

    @Test
    void verifySubscriptionUsesDispatcherContract() {
        VerificationService service = service();
        Subscription subscription = Subscription.builder()
                .id(4L)
                .name("Grace")
                .email("grace@example.com")
                .active(false)
                .status((byte) 0)
                .build();
        VerificationToken token = VerificationToken.builder()
                .token("token-456")
                .subscription(subscription)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();
        when(tokenRepository.findByToken("token-456")).thenReturn(Optional.of(token));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.callable(
                "verifySubscription",
                VerificationRequest.builder().token("token-456").build());

        assertThat(response).singleElement().isInstanceOfSatisfying(VerificationResponse.class, item -> {
            assertThat(item.getId()).isEqualTo(4L);
            assertThat(item.getName()).isEqualTo("Grace");
            assertThat(item.isActive()).isTrue();
            assertThat(item.getStatus()).isEqualTo((byte) 1);
        });
    }

    @Test
    void verifyThrowsForMissingUsedAndExpiredTokens() {
        VerificationService service = service();
        when(tokenRepository.findByToken("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verify("missing"))
                .isInstanceOf(TokenNotFoundException.class);

        VerificationToken used = VerificationToken.builder()
                .token("used")
                .subscription(Subscription.builder().build())
                .createdAt(LocalDateTime.now().minusHours(1))
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(true)
                .build();
        when(tokenRepository.findByToken("used")).thenReturn(Optional.of(used));
        assertThatThrownBy(() -> service.verify("used"))
                .isInstanceOf(TokenAlreadyUsedException.class);

        VerificationToken expired = VerificationToken.builder()
                .token("expired")
                .subscription(Subscription.builder().build())
                .createdAt(LocalDateTime.now().minusHours(2))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();
        when(tokenRepository.findByToken("expired")).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.verify("expired"))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void verifySubscriptionRejectsBlankTokenAndUnknownService() {
        VerificationService service = service();

        assertThatThrownBy(() -> service.verifySubscription(
                VerificationRequest.builder().token(" ").build()))
                .isInstanceOf(IncorrectRequestSentException.class)
                .hasMessageContaining("Verification token is required");

        assertThatThrownBy(() -> service.callable("missing", VerificationRequest.builder().build()))
                .isInstanceOf(ServiceNameNotFoundException.class)
                .hasMessageContaining("Service name not found");
    }

    private VerificationService service() {
        VerificationService service = new VerificationService(
                tokenRepository,
                subscriptionRepository,
                new ExceptionAdvice());
        ReflectionTestUtils.setField(service, "expiryHours", 24);
        return service;
    }
}
