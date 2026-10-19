package com.example.quanlydetai.notification;

import static org.assertj.core.api.Assertions.*;

import com.example.quanlydetai.entity.ResultEmail;
import com.example.quanlydetai.repository.ResultEmailRepository;
import com.example.quanlydetai.security.AccountPrincipal;
import com.example.quanlydetai.service.AcademicRules;
import com.example.quanlydetai.service.DiemService;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @Autowired JdbcTemplate jdbc;
    @Autowired DiemService grades;
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

    @Test
    void committedPublicationCreatesAndDeliversOneResultPerGroupMember() throws Exception {
        var tx = new TransactionTemplate(manager);
        try {
            tx.executeWithoutResult(
                    ignored -> {
                        var now = rules.now();
                        jdbc.update(
                                "INSERT INTO BO_MON(Ma_Bo_Mon,Ten_Bo_Mon) VALUES ('SMTP_BM','FIT"
                                        + " SMTP')");
                        jdbc.update(
                                "INSERT INTO GV(Ma_GV,Ho_Ten,Email,Ma_Bo_Mon) VALUES"
                                    + " ('SMTP_GV','Trưởng khoa','dean@example.test','SMTP_BM')");
                        jdbc.update(
                                "INSERT INTO TRUONG_KHOA(Ma_GV,Dang_Giu_Chuc) VALUES"
                                        + " ('SMTP_GV',true)");
                        jdbc.update(
                                "INSERT INTO"
                                    + " TAI_KHOAN(Ma_TK,Ten_Dang_Nhap,Mat_Khau_Hash,Vai_Tro,Trang_Thai,Ngay_Tao,Ma_GV)"
                                    + " VALUES"
                                    + " ('SMTP_TK','smtp.dean','unused-in-service-test','GV','HoatDong',?,'SMTP_GV')",
                                now);
                        jdbc.update(
                                "INSERT INTO"
                                    + " DOT_DANG_KY(Ma_Dot,Ma_Truong_Khoa,Ten_Dot_Dang_Ky,Loai,Begin_GV,End_GV,Begin_SV,End_SV)"
                                    + " VALUES ('SMTP_DOT','SMTP_GV','Đồ án FIT','MonHoc',?,?,?,?)",
                                now.minusDays(10),
                                now.minusDays(8),
                                now.minusDays(7),
                                now.minusDays(1));
                        jdbc.update(
                                "INSERT INTO"
                                    + " DE_TAI(Ma_De_Tai,Ten_De_Tai,Ma_Bo_Mon,Noi_Dung,Trang_Thai,Ma_Dot,Ma_GV_Dang_Ky,Ngay_Dang_Ky)"
                                    + " VALUES ('SMTP_TOPIC',?,'SMTP_BM','Kết quả kiểm"
                                    + " thử','HoanThanh','SMTP_DOT','SMTP_GV',?)",
                                "Quản lý đồ án FIT " + marker,
                                now.minusDays(9));
                        jdbc.update(
                                "INSERT INTO"
                                    + " NHOM(Ma_Nhom,Ten_Nhom,Ma_Dot,Ma_De_Tai,Ngay_Chon_De_Tai)"
                                    + " VALUES ('SMTP_NHOM','Nhóm FIT','SMTP_DOT','SMTP_TOPIC',?)",
                                now.minusDays(3));
                        for (int i = 1; i <= 3; i++) {
                            jdbc.update(
                                    "INSERT INTO SV(Ma_SV,Ho_Ten,Email) VALUES (?,?,?)",
                                    "SMTP_SV" + i,
                                    "Sinh viên " + i,
                                    "smtp.sv" + i + "@example.test");
                            jdbc.update(
                                    "INSERT INTO THANH_VIEN_NHOM(Ma_Nhom,Ma_SV,Vai_Tro) VALUES"
                                            + " ('SMTP_NHOM',?,?)",
                                    "SMTP_SV" + i,
                                    i == 1 ? "Truong_Nhom" : "Thanh_Vien");
                        }
                        jdbc.update(
                                "INSERT INTO"
                                    + " KET_QUA_DE_TAI(Ma_De_Tai,Diem_Cuoi,Ma_GV_Tong_Hop,Trang_Thai_Cong_Bo,Ngay_Tong_Hop)"
                                    + " VALUES ('SMTP_TOPIC',8.25,'SMTP_GV','ChuaCongBo',?)",
                                now);
                    });
            var principal =
                    new AccountPrincipal(
                            "SMTP_TK",
                            "smtp.dean",
                            "unused-in-service-test",
                            true,
                            "SMTP_GV",
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_TRUONGKHOA")));
            SecurityContextHolder.getContext()
                    .setAuthentication(
                            UsernamePasswordAuthenticationToken.authenticated(
                                    principal, null, principal.getAuthorities()));
            grades.publish("SMTP_TOPIC");
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT Trang_Thai_Cong_Bo FROM KET_QUA_DE_TAI WHERE"
                                            + " Ma_De_Tai='SMTP_TOPIC'",
                                    String.class))
                    .isEqualTo("DaCongBo");
            assertThat(emails.findAll())
                    .hasSize(3)
                    .allSatisfy(
                            e -> {
                                assertThat(e.getState()).isEqualTo("PENDING");
                                assertThat(e.getBody())
                                        .contains("8.25/10", "period=SMTP_DOT", marker);
                            });
            var worker = new EmailDeliveryWorker(queue, transport, true);
            worker.deliverBatch();
            assertThat(emails.findAll())
                    .hasSize(3)
                    .allSatisfy(e -> assertThat(e.getState()).isEqualTo("ACCEPTED"));
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
                assertThat(mail.get("Subject")).isEqualTo("FIT - Kết quả đề tài đã được công bố");
                assertThat((String) mail.get("Text"))
                        .contains("8.25/10", "period=SMTP_DOT", marker);
            }
            assertThat(recipients)
                    .containsExactlyInAnyOrder(
                            "smtp.sv1@example.test",
                            "smtp.sv2@example.test",
                            "smtp.sv3@example.test");
            assertThatThrownBy(() -> grades.publish("SMTP_TOPIC"))
                    .hasMessageContaining("Kết quả đã công bố");
            worker.deliverBatch();
            assertThat(emails.count()).isEqualTo(3);
            assertThat(received()).hasSize(3);
        } finally {
            SecurityContextHolder.clearContext();
            tx.executeWithoutResult(
                    ignored -> {
                        emails.deleteAllInBatch();
                        jdbc.update("DELETE FROM KET_QUA_DE_TAI WHERE Ma_De_Tai='SMTP_TOPIC'");
                        jdbc.update("DELETE FROM THANH_VIEN_NHOM WHERE Ma_Nhom='SMTP_NHOM'");
                        jdbc.update("DELETE FROM NHOM WHERE Ma_Nhom='SMTP_NHOM'");
                        jdbc.update("DELETE FROM DE_TAI WHERE Ma_De_Tai='SMTP_TOPIC'");
                        jdbc.update("DELETE FROM DOT_DANG_KY WHERE Ma_Dot='SMTP_DOT'");
                        jdbc.update("DELETE FROM TAI_KHOAN WHERE Ma_TK='SMTP_TK'");
                        jdbc.update("DELETE FROM TRUONG_KHOA WHERE Ma_GV='SMTP_GV'");
                        jdbc.update("DELETE FROM GV WHERE Ma_GV='SMTP_GV'");
                        jdbc.update(
                                "DELETE FROM SV WHERE Ma_SV IN ('SMTP_SV1','SMTP_SV2','SMTP_SV3')");
                        jdbc.update("DELETE FROM BO_MON WHERE Ma_Bo_Mon='SMTP_BM'");
                    });
        }
    }
}
