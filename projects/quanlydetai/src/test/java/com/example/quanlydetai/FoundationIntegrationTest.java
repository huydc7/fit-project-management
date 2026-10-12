package com.example.quanlydetai;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.quanlydetai.entity.*;
import com.example.quanlydetai.security.CustomUserDetailsService;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FoundationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired EntityManager em;
    @Autowired CustomUserDetailsService users;
    private static String hash;

    @BeforeEach
    void seedAccounts() {
        if (hash == null) hash = encoder.encode("Test-only-Password!");
        jdbc.update(
                "INSERT INTO BO_MON (Ma_Bo_Mon,Ten_Bo_Mon) VALUES (?,?)",
                "TEST_BM",
                "Test department");
        jdbc.update(
                "INSERT INTO GV (Ma_GV,Ho_Ten,Email,SDT,Ma_Bo_Mon) VALUES (?,?,?,?,?)",
                "TEST_DEAN",
                "Dean",
                "dean@example.test",
                null,
                "TEST_BM");
        jdbc.update(
                "INSERT INTO GV (Ma_GV,Ho_Ten,Email,SDT,Ma_Bo_Mon) VALUES (?,?,?,?,?)",
                "TEST_GV",
                "Teacher",
                "gv@example.test",
                null,
                "TEST_BM");
        jdbc.update("INSERT INTO TRUONG_KHOA(Ma_GV,Dang_Giu_Chuc) VALUES (?,true)", "TEST_DEAN");
        jdbc.update(
                "INSERT INTO SV (Ma_SV,Ho_Ten,Email,SDT) VALUES (?,?,?,?)",
                "TEST_SV",
                "Student",
                "sv@example.test",
                null);
        account("TEST_TK_DEAN", "dean", "GV", "TEST_DEAN", null);
        account("TEST_TK_GV", "teacher", "GV", "TEST_GV", null);
        account("TEST_TK_SV", "student", "SV", null, "TEST_SV");
        account("TEST_TK_ADMIN", "administrator", "Admin", null, null);
    }

    private void account(String id, String name, String role, String gv, String sv) {
        jdbc.update(
                "INSERT INTO TAI_KHOAN"
                    + " (Ma_TK,Ten_Dang_Nhap,Mat_Khau_Hash,Vai_Tro,Trang_Thai,Ma_GV,Ma_SV,Ngay_Tao)"
                    + " VALUES (?,?,?,?,?,?,?,?)",
                id,
                name,
                hash,
                role,
                "HoatDong",
                gv,
                sv,
                LocalDateTime.now());
    }

    private MockHttpSession login(String name) throws Exception {
        return (MockHttpSession)
                mvc.perform(formLogin().user(name).password("Test-only-Password!"))
                        .andExpect(authenticated().withUsername(name))
                        .andReturn()
                        .getRequest()
                        .getSession(false);
    }

    @Test
    void authenticatesWithBcryptAndRejectsWrongPassword() throws Exception {
        // Optional SMTP is deliberately unavailable in this test environment.
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        login("student");
        mvc.perform(formLogin().user("student").password("wrong")).andExpect(unauthenticated());
    }

    @Test
    void deanDerivesAuthorityFromDatabaseAndAdminDoesNotInheritIt() throws Exception {
        mvc.perform(get("/dean/periods").session(login("dean"))).andExpect(status().isOk());
        mvc.perform(get("/dean/periods").session(login("teacher")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/dean/periods").session(login("administrator")))
                .andExpect(status().isForbidden());
    }

    @Test
    void bothTopicUrlsRequireStudentAndRenderDtoWithoutOpenSessionInView() throws Exception {
        mvc.perform(get("/student/topics").session(login("student"))).andExpect(status().isOk());
        mvc.perform(get("/topics").session(login("teacher"))).andExpect(status().isForbidden());
    }

    @Test
    void oldBookmarksRedirectToEnglishPagesWithoutBypassingRoles() throws Exception {
        var student = login("student");
        mvc.perform(get("/thong-bao").session(student))
                .andExpect(status().isMovedPermanently())
                .andExpect(redirectedUrl("/announcements"));
        mvc.perform(get("/sv/nhom").session(student))
                .andExpect(status().isMovedPermanently())
                .andExpect(redirectedUrl("/student/groups"));
        mvc.perform(get("/admin/tai-khoan").session(student)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/accounts").session(student)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/tai-khoan").session(login("administrator")))
                .andExpect(status().isMovedPermanently())
                .andExpect(redirectedUrl("/admin/accounts"));
        mvc.perform(get("/truong-khoa/dot").session(login("teacher")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/truong-khoa/dot").session(login("dean")))
                .andExpect(status().isMovedPermanently())
                .andExpect(redirectedUrl("/dean/periods"));
    }

    @Test
    void navigationAndFormsUseEnglishUrlsAndKeepVietnameseLabels() throws Exception {
        var studentHtml =
                mvc.perform(get("/student/groups").session(login("student")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(studentHtml)
                .containsPattern(
                        "<a\\b(?=[^>]*href=\"/student/groups\")(?=[^>]*class=\"[^\"]*\\bactive\\b)[^>]*>")
                .containsPattern("<form\\b[^>]*action=\"/student/groups\"")
                .containsPattern(
                        "<a\\b[^>]*href=\"/announcements\"[^>]*>(?s:.*?)<span>Thông báo</span>")
                .doesNotContain("href=\"/sv/");
        var adminHtml =
                mvc.perform(get("/admin/accounts").session(login("administrator")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(adminHtml)
                .containsPattern(
                        "<a\\b(?=[^>]*href=\"/admin/accounts\")(?=[^>]*class=\"[^\"]*\\bactive\\b)[^>]*>")
                .containsPattern("<form\\b[^>]*action=\"/admin/accounts\"");
    }

    @Test
    void accountLockInvalidatesExistingSession() throws Exception {
        var session = login("student");
        jdbc.update("UPDATE TAI_KHOAN SET Trang_Thai='Khoa' WHERE Ma_TK='TEST_TK_SV'");
        em.clear();
        mvc.perform(get("/").session(session)).andExpect(redirectedUrl("/login?expired"));
        mvc.perform(formLogin().user("student").password("Test-only-Password!"))
                .andExpect(unauthenticated());
    }

    @Test
    void revokedDeanLosesPermissionOnNextRequest() throws Exception {
        var session = login("dean");
        jdbc.update("DELETE FROM TRUONG_KHOA WHERE Ma_GV='TEST_DEAN'");
        em.clear();
        mvc.perform(get("/dean/periods").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void csrfRequiredForMutation() throws Exception {
        mvc.perform(post("/dean/periods").session(login("dean"))).andExpect(status().isForbidden());
    }

    @Test
    void creatorCannotBeSpoofedAndDuplicateDoesNotOverwrite() throws Exception {
        var session = login("dean");
        mvc.perform(
                        post("/dean/periods")
                                .session(session)
                                .with(csrf())
                                .param("maDot", "TEST_DOT")
                                .param("maTruongKhoa", "TEST_GV")
                                .param("tenDotDangKy", "Test period")
                                .param("loai", "MonHoc")
                                .param("beginGV", "2099-01-01T08:00")
                                .param("endGV", "2099-01-02T08:00")
                                .param("beginSV", "2099-01-03T08:00")
                                .param("endSV", "2099-01-04T08:00"))
                .andExpect(redirectedUrl("/dean/periods"))
                .andExpect(flash().attributeExists("thanhCong"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Ma_Truong_Khoa FROM DOT_DANG_KY WHERE Ma_Dot='TEST_DOT'",
                                String.class))
                .isEqualTo("TEST_DEAN");
        mvc.perform(
                        post("/dean/periods")
                                .session(session)
                                .with(csrf())
                                .param("maDot", "TEST_DOT")
                                .param("tenDotDangKy", "Changed")
                                .param("loai", "MonHoc")
                                .param("beginGV", "2099-01-01T08:00")
                                .param("endGV", "2099-01-02T08:00")
                                .param("beginSV", "2099-01-03T08:00")
                                .param("endSV", "2099-01-04T08:00"))
                .andExpect(flash().attributeExists("loi"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Ten_Dot_Dang_Ky FROM DOT_DANG_KY WHERE Ma_Dot='TEST_DOT'",
                                String.class))
                .isEqualTo("Test period");
    }

    @Test
    void invalidPeriodReturnsFieldErrorsWithoutInsert() throws Exception {
        mvc.perform(
                        post("/dean/periods")
                                .session(login("dean"))
                                .with(csrf())
                                .param("maDot", "INVALID"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "beginGV", "loai"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM DOT_DANG_KY WHERE Ma_Dot='INVALID'",
                                Integer.class))
                .isZero();
    }

    @Test
    void entityEqualityWorksWithDetachedProxyAndStableHash() {
        BoMon value = new BoMon();
        value.setMaBoMon("TEST_BM");
        int before = value.hashCode();
        var proxy = em.getReference(BoMon.class, "TEST_BM");
        em.detach(proxy);
        assertThat(value).isEqualTo(proxy);
        assertThat(proxy).isEqualTo(value);
        assertThat(value.hashCode()).isEqualTo(before).isEqualTo(proxy.hashCode());
        assertThat(new BoMon()).isNotEqualTo(new BoMon());
    }
}
