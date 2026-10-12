package com.example.quanlydetai.notification;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class ConfiguredEmailTransport implements EmailTransport {
    private final String backend;
    private final String from;
    private final String apiKey;
    private final ObjectProvider<JavaMailSender> smtp;
    private final RestClient http;

    @org.springframework.beans.factory.annotation.Autowired
    public ConfiguredEmailTransport(
            @Value("${app.email.backend:smtp}") String backend,
            @Value("${app.email.from:}") String from,
            @Value("${app.email.api-key:}") String apiKey,
            ObjectProvider<JavaMailSender> smtp) {
        this(backend, from, apiKey, smtp, builder());
    }

    ConfiguredEmailTransport(
            String backend,
            String from,
            String apiKey,
            ObjectProvider<JavaMailSender> smtp,
            RestClient.Builder builder) {
        this.backend = backend;
        this.from = from;
        this.apiKey = apiKey;
        this.smtp = smtp;
        this.http = builder.baseUrl("https://api.brevo.com/v3").build();
    }

    private static RestClient.Builder builder() {
        var factory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(factory);
    }

    @Override
    public String send(String id, String recipient, String subject, String body) {
        if (from.isBlank()) throw new IllegalStateException("EMAIL_FROM is required");
        if ("smtp".equals(backend)) {
            JavaMailSender sender = smtp.getIfAvailable();
            if (sender == null) throw new IllegalStateException("SMTP is not configured");
            var message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            return "smtp-accepted";
        }
        if (!"brevo".equals(backend) || apiKey.isBlank())
            throw new IllegalStateException("Configure EMAIL_BACKEND and EMAIL_API_KEY");
        var response =
                http.post()
                        .uri("/smtp/email")
                        .header("api-key", apiKey)
                        .body(
                                Map.of(
                                        "sender",
                                        Map.of("name", "FIT - Quản lý đề tài", "email", from),
                                        "to",
                                        List.of(Map.of("email", recipient)),
                                        "subject",
                                        subject,
                                        "textContent",
                                        body,
                                        "headers",
                                        Map.of("X-Notification-ID", id)))
                        .retrieve()
                        .body(Map.class);
        if (response == null
                || !(response.get("messageId") instanceof String messageId)
                || messageId.isBlank())
            throw new IllegalStateException("Mail acceptance not confirmed");
        return messageId;
    }
}
