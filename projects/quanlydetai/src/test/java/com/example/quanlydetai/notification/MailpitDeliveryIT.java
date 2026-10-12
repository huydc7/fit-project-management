package com.example.quanlydetai.notification;

import static org.assertj.core.api.Assertions.*;

import com.example.quanlydetai.entity.ResultEmail;
import com.example.quanlydetai.repository.ResultEmailRepository;
import com.example.quanlydetai.service.AcademicRules;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.util.*;

/** Explicit local SMTP check: run tools/Test-LocalEmail.ps1. Not part of default Surefire tests. */
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:mailpitcheck;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.datasource.hikari.connection-init-sql=",
            "app.email.enabled=false",
            "app.email.backend=smtp",
            "app.email.from=fit@example.test",
            "app.email.api-key=",
            "spring.mail.host=127.0.0.1",
            "spring.mail.port=11025",
            "spring.mail.username=",
            "spring.mail.password=",
            "spring.mail.properties.mail.smtp.auth=false",
            "spring.mail.properties.mail.smtp.starttls.enable=false"
        })
@ActiveProfiles("test")
class MailpitDeliveryIT {
    @Autowired ResultEmailRepository emails;
    @Autowired ResultEmailService queue;
    @Autowired EmailTransport transport;
    @Autowired AcademicRules rules;
    @Autowired PlatformTransactionManager manager;
    private final String marker = UUID.randomUUID().toString();
    private final RestClient mailbox = mailbox();

    private static RestClient mailbox() {
        var factory =
                new JdkClientHttpRequestFactory(
                        java.net.http.HttpClient.newBuilder()
                                .connectTimeout(java.time.Duration.ofSeconds(3))
                                .build());
        factory.setReadTimeout(java.time.Duration.ofSeconds(5));
        return RestClient.builder()
                .baseUrl("http://127.0.0.1:18025")
                .requestFactory(factory)
                .build();
    }

    @BeforeEach
    void setup() {
        // Fail clearly when Mailpit is missing; never redirect to an external mail service.
        mailbox.get().uri("/api/v1/info").retrieve().toBodilessEntity();
        emails.deleteAllInBatch();
    }

    @AfterEach
    void cleanup() {
        emails.deleteAllInBatch();
    }

    private ResultEmail notification(int student) {
        var e = new ResultEmail();
        e.setId(UUID.randomUUID().toString().replace("-", ""));
        e.setTopic("SMTP_TOPIC");
        e.setStudent("SMTP_SV" + student);
        e.setRecipient("smtp.sv" + student + "@example.test");
        e.setSubject("FIT - Kết quả đề tài " + marker);
        e.setBody(
                "Chào sinh viên "
                        + student
                        + ",\nĐề tài: Quản lý đồ án khoa Công nghệ thông tin\n"
                        + "Điểm: 8.25/10\nhttp://localhost:8080/student/results?period=SMTP_DOT");
        e.setState("PENDING");
        e.setCreatedAt(rules.now());
        e.setNextAttemptAt(rules.now());
        return e;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> received() {
        var response =
                mailbox.get()
                        .uri(uri -> uri.path("/api/v1/search").queryParam("query", marker).build())
                        .retrieve()
                        .body(Map.class);
        return (List<Map<String, Object>>) response.get("messages");
    }

    @Test
    void sendsThreePrivateVietnameseEmailsThroughRealSmtpWithoutResendingAcceptedRows()
            throws Exception {
        List<ResultEmail> notifications =
                java.util.stream.IntStream.rangeClosed(1, 3).mapToObj(this::notification).toList();
        emails.saveAllAndFlush(notifications);
        new EmailDeliveryWorker(queue, transport, false).deliverBatch();
        assertThat(received()).isEmpty();
        var worker = new EmailDeliveryWorker(queue, transport, true);
        worker.deliverBatch();
        assertThat(emails.findAll())
                .allSatisfy(
                        e -> {
                            assertThat(e.getState()).isEqualTo("ACCEPTED");
                            assertThat(e.getAttempts()).isEqualTo(1);
                        });
        List<Map<String, Object>> messages = received();
        for (int i = 0; messages.size() < 3 && i < 30; i++) {
            Thread.sleep(100);
            messages = received();
        }
        assertThat(messages).hasSize(3);
        Set<String> recipients = new HashSet<>();
        for (var summary : messages) {
            var mail =
                    mailbox.get()
                            .uri("/api/v1/message/{id}", summary.get("ID"))
                            .retrieve()
                            .body(Map.class);
            var to = (List<?>) mail.get("To");
            assertThat(to).hasSize(1);
            recipients.add((String) ((Map<?, ?>) to.getFirst()).get("Address"));
            assertThat((List<?>) mail.get("Cc")).isNullOrEmpty();
            assertThat((List<?>) mail.get("Bcc")).isNullOrEmpty();
            assertThat(mail.get("Subject")).isEqualTo("FIT - Kết quả đề tài " + marker);
            assertThat((String) mail.get("Text"))
                    .contains(
                            "Quản lý đồ án khoa Công nghệ thông tin", "8.25/10", "period=SMTP_DOT");
        }
        assertThat(recipients)
                .containsExactlyInAnyOrder(
                        "smtp.sv1@example.test", "smtp.sv2@example.test", "smtp.sv3@example.test");
        worker.deliverBatch();
        assertThat(received()).hasSize(3);
    }

    @Test
    void rolledBackNotificationNeverReachesSmtp() {
        new TransactionTemplate(manager)
                .executeWithoutResult(
                        tx -> {
                            emails.saveAndFlush(notification(1));
                            tx.setRollbackOnly();
                        });
        new EmailDeliveryWorker(queue, transport, true).deliverBatch();
        assertThat(emails.count()).isZero();
        assertThat(received()).isEmpty();
    }
}
