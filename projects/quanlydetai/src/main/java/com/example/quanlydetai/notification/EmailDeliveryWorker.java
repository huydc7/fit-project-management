package com.example.quanlydetai.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailDeliveryWorker {
    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryWorker.class);
    private final ResultEmailService emails;
    private final EmailTransport transport;
    private final boolean enabled;

    public EmailDeliveryWorker(
            ResultEmailService emails,
            EmailTransport transport,
            @Value("${app.email.enabled:false}") boolean enabled) {
        this.emails = emails;
        this.transport = transport;
        this.enabled = enabled;
    }

    @Scheduled(
            fixedDelayString = "${app.email.poll-ms:15000}",
            initialDelayString = "${app.email.poll-ms:15000}")
    public void deliverBatch() {
        if (!enabled) return;
        for (int i = 0; i < 10; i++) {
            var delivery = emails.claim();
            if (delivery == null) return;
            String receipt;
            try {
                // Network I/O is outside the publication and claim transactions.
                receipt =
                        transport.send(
                                delivery.id(),
                                delivery.recipient(),
                                delivery.subject(),
                                delivery.body());
            } catch (RuntimeException ex) {
                // Do not log recipients, content, credentials or provider response bodies.
                log.warn(
                        "Email {} attempt {} failed ({})",
                        delivery.id(),
                        delivery.attempt(),
                        ex.getClass().getSimpleName());
                emails.complete(delivery.id(), delivery.attempt(), null);
                continue;
            }
            // A DB failure here leaves a lease for recovery; delivery is at least once.
            emails.complete(delivery.id(), delivery.attempt(), receipt);
        }
    }
}
