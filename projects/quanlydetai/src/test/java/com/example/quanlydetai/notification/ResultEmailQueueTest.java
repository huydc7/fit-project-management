package com.example.quanlydetai.notification;

import static org.assertj.core.api.Assertions.*;

import com.example.quanlydetai.entity.ResultEmail;
import com.example.quanlydetai.repository.ResultEmailRepository;
import com.example.quanlydetai.service.AcademicRules;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:emailqueue;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.datasource.hikari.connection-init-sql=",
            "app.email.enabled=false"
        })
@ActiveProfiles("test")
class ResultEmailQueueTest {
    @Autowired ResultEmailService queue;
    @Autowired ResultEmailRepository emails;
    @Autowired AcademicRules rules;
    @Autowired PlatformTransactionManager manager;

    @BeforeEach
    void clean() {
        emails.deleteAllInBatch();
    }

    @AfterEach
    void cleanup() {
        emails.deleteAllInBatch();
    }

    private ResultEmail pending() {
        var e = new ResultEmail();
        e.setId("queue-test");
        e.setTopic("TEST_TOPIC");
        e.setStudent("TEST_SV");
        e.setRecipient("sv@example.test");
        e.setSubject("Kết quả");
        e.setBody("Điểm");
        e.setState("PENDING");
        e.setCreatedAt(rules.now());
        e.setNextAttemptAt(rules.now().minusSeconds(1));
        return emails.saveAndFlush(e);
    }

    @Test
    void activeLeasePreventsSecondClaimAndOldWorkerCannotCompleteNewAttempt() {
        pending();
        var first = queue.claim();
        assertThat(first.attempt()).isEqualTo(1);
        assertThat(queue.claim()).isNull();
        var row = emails.findById(first.id()).orElseThrow();
        row.setNextAttemptAt(rules.now().minusSeconds(1));
        emails.saveAndFlush(row);
        var second = queue.claim();
        assertThat(second.attempt()).isEqualTo(2);
        queue.complete(first.id(), first.attempt(), "late-old-receipt");
        assertThat(emails.findById(first.id()).orElseThrow().getState()).isEqualTo("PROCESSING");
        queue.complete(second.id(), second.attempt(), "accepted");
        assertThat(emails.findById(first.id()).orElseThrow().getState()).isEqualTo("ACCEPTED");
        assertThat(queue.claim()).isNull();
    }

    @Test
    @WithMockUser(roles = "TRUONGKHOA")
    void failuresBackOffThenRequireExplicitRetry() {
        pending();
        for (int i = 1; i <= 5; i++) {
            var delivery = queue.claim();
            assertThat(delivery.attempt()).isEqualTo(i);
            queue.complete(delivery.id(), delivery.attempt(), null);
            assertThat(queue.claim()).isNull();
            var row = emails.findById(delivery.id()).orElseThrow();
            if (i < 5) {
                row.setNextAttemptAt(rules.now().minusSeconds(1));
                emails.saveAndFlush(row);
            }
        }
        assertThat(emails.findById("queue-test").orElseThrow().getState()).isEqualTo("FAILED");
        queue.retry("queue-test");
        assertThat(queue.claim().attempt()).isEqualTo(1);
    }

    @Test
    void rolledBackTransactionLeavesNothingForWorker() {
        new TransactionTemplate(manager)
                .executeWithoutResult(
                        tx -> {
                            pending();
                            tx.setRollbackOnly();
                        });
        assertThat(emails.count()).isZero();
        assertThat(queue.claim()).isNull();
    }
}
