package com.example.quanlydetai;

import static org.assertj.core.api.Assertions.*;

import com.example.quanlydetai.exception.BusinessException;
import com.example.quanlydetai.security.AccountPrincipal;
import com.example.quanlydetai.service.NhomService;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Committed fixtures and two real service transactions; restricted to the isolated local test port.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(
        named = "TEST_DB_URL",
        matches = "jdbc:mysql://(127[.]0[.]0[.]1|localhost):3308/.*")
class MySqlConcurrencyIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired NhomService groups;
    @Autowired PasswordEncoder encoder;
    private static String hash;

    @BeforeEach
    void seed() {
        cleanup();
        if (hash == null) hash = encoder.encode("Concurrency-Test-Password!");
        var now = LocalDateTime.now();
        jdbc.update(
                "INSERT INTO BO_MON(Ma_Bo_Mon,Ten_Bo_Mon) VALUES ('QC_BM','Concurrency test"
                        + " department')");
        jdbc.update(
                "INSERT INTO GV(Ma_GV,Ho_Ten,Email,Ma_Bo_Mon) VALUES ('QC_GV','Test"
                        + " lecturer','qc.gv@example.test','QC_BM')");
        jdbc.update(
                "INSERT INTO"
                    + " TAI_KHOAN(Ma_TK,Ten_Dang_Nhap,Mat_Khau_Hash,Vai_Tro,Ma_GV,Trang_Thai,Ngay_Tao)"
                    + " VALUES ('QC_GV','qc.gv',?,'GV','QC_GV','HoatDong',?)",
                hash,
                now);
        jdbc.update("INSERT INTO TRUONG_KHOA(Ma_GV,Dang_Giu_Chuc) VALUES ('QC_GV',true)");
        jdbc.update(
                "INSERT INTO"
                    + " DOT_DANG_KY(Ma_Dot,Ma_Truong_Khoa,Ten_Dot_Dang_Ky,Loai,Begin_GV,End_GV,Begin_SV,End_SV)"
                    + " VALUES ('QC_DOT','QC_GV','Concurrency test period','MonHoc',?,?,?,?)",
                now.minusDays(10),
                now.minusDays(4),
                now.plusDays(1),
                now.plusDays(5));
        for (String topic : List.of("QC_DT1", "QC_DT2")) {
            jdbc.update(
                    "INSERT INTO"
                        + " DE_TAI(Ma_De_Tai,Ten_De_Tai,Ma_Bo_Mon,Noi_Dung,Trang_Thai,Ma_Dot,Ma_GV_Dang_Ky,Ngay_Dang_Ky)"
                        + " VALUES (?,?,'QC_BM','Concurrency test"
                        + " content','Nhap','QC_DOT','QC_GV',?)",
                    topic,
                    topic,
                    now.minusDays(6));
            jdbc.update("INSERT INTO GVHD_DETAI(Ma_GV,Ma_De_Tai) VALUES ('QC_GV',?)", topic);
            jdbc.update("UPDATE DE_TAI SET Trang_Thai='DaCongBo' WHERE Ma_De_Tai=?", topic);
        }
        jdbc.update("UPDATE DOT_DANG_KY SET Begin_SV=? WHERE Ma_Dot='QC_DOT'", now.minusDays(3));
        for (String group : List.of("QC_N1", "QC_N2"))
            jdbc.update(
                    "INSERT INTO NHOM(Ma_Nhom,Ten_Nhom,Ma_Dot) VALUES (?,?,'QC_DOT')",
                    group,
                    group);
        for (int i = 1; i <= 6; i++) {
            String student = "QC_S" + i;
            jdbc.update(
                    "INSERT INTO SV(Ma_SV,Ho_Ten,Email) VALUES (?,?,?)",
                    student,
                    student,
                    "qc.s" + i + "@example.test");
            jdbc.update(
                    "INSERT INTO"
                        + " TAI_KHOAN(Ma_TK,Ten_Dang_Nhap,Mat_Khau_Hash,Vai_Tro,Ma_SV,Trang_Thai,Ngay_Tao)"
                        + " VALUES (?,?,?,'SV',?,'HoatDong',?)",
                    student,
                    student.toLowerCase(Locale.ROOT),
                    hash,
                    student,
                    now);
            jdbc.update(
                    "INSERT INTO THANH_VIEN_NHOM(Ma_Nhom,Ma_SV,Vai_Tro) VALUES (?,?,?)",
                    i <= 3 ? "QC_N1" : "QC_N2",
                    student,
                    i == 1 || i == 4 ? "Truong_Nhom" : "Thanh_Vien");
        }
    }

    @AfterEach
    void cleanup() {
        jdbc.execute(
                (org.springframework.jdbc.core.ConnectionCallback<Void>)
                        connection -> {
                            if (!connection
                                    .getMetaData()
                                    .getURL()
                                    .matches("jdbc:mysql://(127[.]0[.]0[.]1|localhost):3308/.*"))
                                throw new IllegalStateException(
                                        "Fixture cleanup is restricted to the isolated test port");
                            boolean autoCommit = connection.getAutoCommit();
                            connection.setAutoCommit(false);
                            try (var statement = connection.createStatement()) {
                                statement.executeUpdate(
                                        "DELETE FROM DANG_KY_DE_TAI WHERE Ma_Nhom IN"
                                                + " ('QC_N1','QC_N2')");
                                statement.executeUpdate(
                                        "DELETE FROM THANH_VIEN_NHOM WHERE Ma_Nhom IN"
                                                + " ('QC_N1','QC_N2')");
                                statement.executeUpdate(
                                        "DELETE FROM NHOM WHERE Ma_Nhom IN ('QC_N1','QC_N2')");
                                // Published histories cannot be deleted through normal business
                                // operations.
                                // Reset exact fixture IDs on this one connection only; all
                                // constraints remain
                                // enabled while the concurrent service transactions run. Never use
                                // this in app code.
                                statement.execute("SET FOREIGN_KEY_CHECKS=0");
                                statement.executeUpdate(
                                        "DELETE FROM DE_TAI WHERE Ma_De_Tai IN"
                                                + " ('QC_DT1','QC_DT2')");
                                statement.executeUpdate(
                                        "DELETE FROM GVHD_DETAI WHERE Ma_De_Tai IN"
                                                + " ('QC_DT1','QC_DT2')");
                                statement.execute("SET FOREIGN_KEY_CHECKS=1");
                                statement.executeUpdate(
                                        "DELETE FROM DOT_DANG_KY WHERE Ma_Dot='QC_DOT'");
                                statement.executeUpdate(
                                        "DELETE FROM TRUONG_KHOA WHERE Ma_GV='QC_GV'");
                                statement.executeUpdate(
                                        "DELETE FROM TAI_KHOAN WHERE Ma_TK IN"
                                            + " ('QC_GV','QC_S1','QC_S2','QC_S3','QC_S4','QC_S5','QC_S6')");
                                statement.executeUpdate(
                                        "DELETE FROM SV WHERE Ma_SV IN"
                                            + " ('QC_S1','QC_S2','QC_S3','QC_S4','QC_S5','QC_S6')");
                                statement.executeUpdate("DELETE FROM GV WHERE Ma_GV='QC_GV'");
                                statement.executeUpdate(
                                        "DELETE FROM BO_MON WHERE Ma_Bo_Mon='QC_BM'");
                                connection.commit();
                            } catch (java.sql.SQLException | RuntimeException ex) {
                                connection.rollback();
                                throw ex;
                            } finally {
                                try (var reset = connection.createStatement()) {
                                    reset.execute("SET FOREIGN_KEY_CHECKS=1");
                                }
                                connection.setAutoCommit(autoCommit);
                            }
                            return null;
                        });
    }

    @Test
    void twoGroupsCannotReserveTheSameTopicConcurrently() throws Exception {
        assertOneWinner(
                new Request("QC_S1", "QC_N1", "QC_DT1"), new Request("QC_S4", "QC_N2", "QC_DT1"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM DANG_KY_DE_TAI WHERE Active_Topic='QC_DT1'",
                                Integer.class))
                .isEqualTo(1);
    }

    @Test
    void oneGroupCannotReserveTwoDifferentTopicsConcurrently() throws Exception {
        assertOneWinner(
                new Request("QC_S1", "QC_N1", "QC_DT1"), new Request("QC_S1", "QC_N1", "QC_DT2"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM DANG_KY_DE_TAI WHERE Active_Group='QC_N1'",
                                Integer.class))
                .isEqualTo(1);
    }

    private record Request(String student, String group, String topic) {}

    private void assertOneWinner(Request first, Request second) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> attempt(first, ready, start));
            var b = pool.submit(() -> attempt(second, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM DANG_KY_DE_TAI WHERE Ma_Nhom IN"
                                            + " ('QC_N1','QC_N2')",
                                    Integer.class))
                    .isEqualTo(1);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM NHOM WHERE Ma_Nhom IN ('QC_N1','QC_N2')"
                                            + " AND Ma_De_Tai IS NOT NULL",
                                    Integer.class))
                    .isZero();
        } finally {
            start.countDown();
            pool.shutdownNow();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private boolean attempt(Request request, CountDownLatch ready, CountDownLatch start)
            throws Exception {
        var principal =
                new AccountPrincipal(
                        request.student(),
                        request.student().toLowerCase(Locale.ROOT),
                        hash,
                        true,
                        null,
                        request.student(),
                        List.of(new SimpleGrantedAuthority("ROLE_SV")));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
        try {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS))
                throw new IllegalStateException("Start barrier timed out");
            groups.register(request.group(), request.topic());
            return true;
        } catch (BusinessException | DataIntegrityViolationException rejected) {
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
