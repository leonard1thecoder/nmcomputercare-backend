package com.backend.nmcomputercare.newsletter.mailing;


import com.backend.nmcomputercare.newsletter.dtos.NewsletterPublishedEvent;
import com.backend.nmcomputercare.subscribe.entity.Subscription;
import com.backend.nmcomputercare.subscribe.repository.SubscriptionRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Listens for {@link NewsletterPublishedEvent} and dispatches an HTML email
 * to every active subscriber.
 *
 * <h3>Threading</h3>
 * The listener is annotated with {@code @Async} so it never blocks the
 * request thread that published the event.  Ensure {@code @EnableAsync} is
 * present on a {@code @Configuration} class (typically your main application
 * class or a dedicated {@code AsyncConfig}).
 *
 * <h3>Failure isolation</h3>
 * A per-subscriber try/catch ensures that one bad address does not prevent
 * the remaining subscribers from receiving their copies.  Failures are
 * logged as warnings; they do not roll back the newsletter creation
 * transaction.
 */
@Component
@RequiredArgsConstructor
public class NewsletterEmailListener {

    private static final Logger logger = LoggerFactory.getLogger(NewsletterEmailListener.class);

    private final JavaMailSender          mailSender;
    private final SubscriptionRepository subscriptionRepository;

    @Value("${spring.mail.username}")
    private String senderAddress;

    /**
     * Base URL used to build the unsubscribe deep-link embedded in the email.
     * Configure in {@code application.yml}:
     * <pre>app.base-url=https://www.nmcomputercare.co.za</pre>
     */
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // ──────────────────────────────────────────────────────────────────────────

    @Async
    @EventListener
    public void onNewsletterPublished(NewsletterPublishedEvent event) {

        List<Subscription> activeSubscribers = subscriptionRepository.findByActive(true);

        if (activeSubscribers.isEmpty()) {
            logger.info("Newsletter published but no active subscribers | newsletterId={}",
                    event.getId());
            return;
        }

        logger.info("Dispatching newsletter email | newsletterId={} title={} recipients={}",
                event.getId(), event.getTitle(), activeSubscribers.size());

        int successCount = 0;
        int failCount    = 0;

        for (Subscription subscriber : activeSubscribers) {
            try {
                sendTo(subscriber, event);
                successCount++;
                logger.debug("Newsletter email sent | newsletterId={} email={}",
                        event.getId(), maskEmail(subscriber.getEmail()));
            } catch (Exception ex) {
                failCount++;
                logger.warn("Failed to send newsletter email | newsletterId={} email={} reason={}",
                        event.getId(), maskEmail(subscriber.getEmail()), ex.getMessage());
            }
        }

        logger.info("Newsletter dispatch complete | newsletterId={} sent={} failed={}",
                event.getId(), successCount, failCount);
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void sendTo(Subscription subscriber, NewsletterPublishedEvent event)
            throws MessagingException, UnsupportedEncodingException {

        String unsubscribeLink = buildUnsubscribeLink(subscriber.getEmail());

        String htmlBody = NewsletterEmailTemplate.build(
                subscriber.getName() != null ? subscriber.getName() : "Subscriber",
                event.getTitle(),
                event.getContent(),
                unsubscribeLink);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(senderAddress, "NM Computer Care");
        helper.setTo(subscriber.getEmail());
        helper.setSubject("📬 " + event.getTitle() + " | NM Computer Care");
        helper.setText(htmlBody, true); // true = isHtml
        helper.setReplyTo(senderAddress);

        mailSender.send(message);
    }

    private String buildUnsubscribeLink(String email) {
        // Deep-link to the frontend unsubscribe page, or directly to the API
        // endpoint.  Adjust to match your frontend routing.
        return baseUrl + "/unsubscribe?email=" + java.net.URLEncoder
                .encode(email, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local  = parts[0];
        String masked = local.length() > 2 ? local.substring(0, 2) + "***" : "***";
        return masked + "@" + parts[1];
    }
}