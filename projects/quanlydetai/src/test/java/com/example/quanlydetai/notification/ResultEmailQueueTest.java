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

    @Test
    void exhaustedExpiredLeaseDoesNotStopOtherRecipientsInTheSameBatch() {
        var expired = pending();
        expired.setState("PROCESSING");
        expired.setAttempts(5);
        expired.setCreatedAt(rules.now().minusDays(1));
        emails.saveAndFlush(expired);
        var active = new ResultEmail();
        active.setId("active-lease");
        active.setTopic("TEST_TOPIC");
        active.setStudent("ACTIVE_SV");
        active.setRecipient("active@example.test");
        active.setSubject("Kết quả");
        active.setBody("Điểm");
        active.setState("PROCESSING");
        active.setAttempts(5);
        active.setCreatedAt(rules.now().minusHours(1));
        active.setNextAttemptAt(rules.now().plusMinutes(2));
        emails.saveAndFlush(active);
        for (int i = 1; i <= 9; i++) {
            var row = new ResultEmail();
            row.setId("ready-" + i);
            row.setTopic("TEST_TOPIC");
            row.setStudent("TEST_SV" + i);
            row.setRecipient("sv" + i + "@example.test");
            row.setSubject("Kết quả");
            row.setBody("Điểm");
            row.setState("PENDING");
            row.setCreatedAt(rules.now());
            row.setNextAttemptAt(rules.now().minusSeconds(1));
            emails.saveAndFlush(row);
        }
        new EmailDeliveryWorker(queue, (id, to, subject, body) -> "local-accepted", true)
                .deliverBatch();
        assertThat(emails.findById("queue-test").orElseThrow().getState()).isEqualTo("FAILED");
        assertThat(emails.findById("active-lease").orElseThrow().getState())
                .isEqualTo("PROCESSING");
        assertThat(emails.findAll().stream().filter(e -> e.getId().startsWith("ready-")))
                .hasSize(9)
                .allSatisfy(
                        e -> {
                            assertThat(e.getState()).isEqualTo("ACCEPTED");
                            assertThat(e.getAttempts()).isEqualTo(1);
                        });
    }

    @Test
    @WithMockUser(roles = "TRUONGKHOA")
    void filtersBeforePaginationAndCanFindOldEmailsBeyondThePreviousHundredRowLimit() {
        for (int i = 0; i < 105; i++) {
            var row = new ResultEmail();
            row.setId("search-" + i);
            row.setTopic("SEARCH_TOPIC");
            row.setStudent("SEARCH_SV" + i);
            row.setRecipient("sv" + i + "@example.test");
            row.setSubject("Không đưa nội dung này vào API");
            row.setBody("Điểm riêng tư");
            row.setState(i == 104 ? "FAILED" : "PENDING");
            row.setCreatedAt(rules.now().minusDays(i));
            row.setNextAttemptAt(rules.now());
            emails.save(row);
        }
        emails.flush();
        var oldest = queue.search(" search_sv104 ", "failed", 0, 20);
        assertThat(oldest.totalElements()).isEqualTo(1);
        assertThat(oldest.content())
                .extracting(ResultEmailService.Status::id)
                .containsExactly("search-104");
        var page = queue.search("", "PENDING", 1, 20);
        assertThat(page.totalElements()).isEqualTo(104);
        assertThat(page.totalPages()).isEqualTo(6);
        assertThat(page.content()).hasSize(20);
        assertThat(queue.search("SEARCH_%", "", 0, 20).totalElements()).isZero();
        assertThat(queue.search("", "ACCEPTED", 0, 20).content()).isEmpty();
    }
}
