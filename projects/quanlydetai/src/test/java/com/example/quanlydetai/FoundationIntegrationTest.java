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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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
        // Website health depends on the application and database.
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

    @ParameterizedTest
    @ValueSource(strings = {"2098-12-01T08:00", "2099-01-04T07:59", "2099-01-04T08:00"})
    void assessmentDeadlineMustFollowStudentRegistration(String deadline) throws Exception {
        for (String type : java.util.List.of("TLCN", "KLTN")) {
            assertInvalidSchedule(type, "deadlineGvpbNopDiem", deadline, "Hạn chót GVPB");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"2098-12-01T08:00", "2099-01-04T07:59", "2099-01-04T08:00"})
    void councilReportMustFollowStudentRegistration(String reportTime) throws Exception {
        assertInvalidSchedule("KLTN", "timeBcHd", reportTime, "Ngày báo cáo hội đồng");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2099-01-06T08:00", "2099-01-06T08:01"})
    void reviewerDeadlineMustBeStrictlyBeforeCouncilReport(String deadline) throws Exception {
        assertInvalidSchedule(
                "KLTN", "deadlineGvpbNopDiem", deadline,
                "Hạn chót GVPB nộp điểm phải trước thời điểm báo cáo hội đồng.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2099-01-05T07:59", "2099-01-05T08:00"})
    void movingCouncilReportCannotInvalidateReviewerDeadline(String reportTime) throws Exception {
        assertInvalidSchedule(
                "KLTN", "timeBcHd", reportTime,
                "Hạn chót GVPB nộp điểm phải trước thời điểm báo cáo hội đồng.");
    }

    @Test
    void reviewerDeadlineOneMinuteBeforeCouncilReportIsValidForCreateAndEdit() throws Exception {
        var session = login("dean");
        var form = periodForm("BEFORE_COUNCIL", "KLTN");
        form.set("deadlineGvpbNopDiem", "2099-01-06T07:59");
        mvc.perform(post("/dean/periods").session(session).with(csrf()).params(form))
                .andExpect(redirectedUrl("/dean/periods"))
                .andExpect(flash().attributeExists("thanhCong"));
        form.set("tenDotDangKy", "Lịch hợp lệ sát giờ hội đồng");
        mvc.perform(post("/dean/periods/BEFORE_COUNCIL/edit")
                        .session(session).with(csrf()).params(form))
                .andExpect(redirectedUrl("/dean/periods"))
                .andExpect(flash().attributeExists("thanhCong"));
        assertThat(jdbc.queryForObject(
                "SELECT Deadline_GVPB_Nop_Diem FROM DOT_DANG_KY WHERE Ma_Dot='BEFORE_COUNCIL'",
                LocalDateTime.class)).isEqualTo(LocalDateTime.parse("2099-01-06T07:59"));
    }

    private MultiValueMap<String, String> periodForm(String id, String type) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("maDot", id);
        form.add("tenDotDangKy", "Đợt kiểm tra lịch");
        form.add("loai", type);
        form.add("beginGV", "2099-01-01T08:00");
        form.add("endGV", "2099-01-02T08:00");
        form.add("beginSV", "2099-01-03T08:00");
        form.add("endSV", "2099-01-04T08:00");
        if (type.equals("TLCN") || type.equals("KLTN"))
            form.add("deadlineGvpbNopDiem", "2099-01-05T08:00");
        if (type.equals("KLTN")) form.add("timeBcHd", "2099-01-06T08:00");
        return form;
    }

    private void assertInvalidSchedule(String type, String field, String value, String message)
            throws Exception {
        var session = login("dean");
        String id = "SCHEDULE_" + type;
        var form = periodForm(id, type);
        mvc.perform(post("/dean/periods").session(session).with(csrf()).params(form))
                .andExpect(flash().attributeExists("thanhCong"));
        String original = form.getFirst(field);
        form.set(field, value);
        mvc.perform(post("/dean/periods/" + id + "/edit")
                        .session(session).with(csrf()).params(form))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loi"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(message)));
        mvc.perform(put("/api/v1/registration-periods/" + id)
                        .session(session).with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(tools.jackson.databind.json.JsonMapper.builder().build()
                                .writeValueAsString(form.toSingleValueMap())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(message)));
        em.clear();
        var unchanged = em.find(DotDangKy.class, id);
        assertThat(field.equals("timeBcHd") ? unchanged.getTimeBcHd()
                : unchanged.getDeadlineGvpbNopDiem()).isEqualTo(LocalDateTime.parse(original));

        String newId = "NEW_" + type;
        form.set("maDot", newId);
        mvc.perform(post("/dean/periods").session(session).with(csrf()).params(form))
                .andExpect(redirectedUrl("/dean/periods"))
                .andExpect(flash().attributeExists("loi"))
                .andExpect(result -> assertThat(result.getFlashMap().containsKey("thanhCong")).isFalse());
        mvc.perform(post("/api/v1/registration-periods")
                        .session(session).with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(tools.jackson.databind.json.JsonMapper.builder().build()
                                .writeValueAsString(form.toSingleValueMap())))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM DOT_DANG_KY WHERE Ma_Dot=?",
                Integer.class, newId)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MonHoc", "NCKH", "TLCN", "KLTN"})
    void validSchedulesCanBeCreatedAndEdited(String type) throws Exception {
        var session = login("dean");
        var form = periodForm("VALID_" + type, type);
        mvc.perform(post("/dean/periods").session(session).with(csrf()).params(form))
                .andExpect(redirectedUrl("/dean/periods"))
                .andExpect(flash().attributeExists("thanhCong"));
        form.set("tenDotDangKy", "Đợt đã sửa tên");
        mvc.perform(post("/dean/periods/VALID_" + type + "/edit")
                        .session(session).with(csrf()).params(form))
                .andExpect(redirectedUrl("/dean/periods"))
                .andExpect(flash().attributeExists("thanhCong"));
        assertThat(jdbc.queryForObject("SELECT Ten_Dot_Dang_Ky FROM DOT_DANG_KY WHERE Ma_Dot=?",
                String.class, "VALID_" + type)).isEqualTo("Đợt đã sửa tên");
    }

    @ParameterizedTest
    @ValueSource(strings = {"beginGV", "endGV", "beginSV", "endSV"})
    void missingRequiredDateOnEditShowsValidationMessageAndKeepsForm(String field) throws Exception {
        var session = login("dean");
        var form = periodForm("EDIT_REQUIRED", "MonHoc");
        mvc.perform(post("/dean/periods").session(session).with(csrf()).params(form))
                .andExpect(flash().attributeExists("thanhCong"));
        form.remove(field);
        mvc.perform(post("/dean/periods/EDIT_REQUIRED/edit")
                        .session(session).with(csrf()).params(form))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", field))
                .andExpect(model().attribute("editingId", "EDIT_REQUIRED"))
                .andExpect(model().attributeExists("loi"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Phải nhập thời gian")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/dean/periods/EDIT_REQUIRED/edit")));
        assertThat(jdbc.queryForObject("SELECT Begin_GV FROM DOT_DANG_KY WHERE Ma_Dot='EDIT_REQUIRED'",
                LocalDateTime.class)).isEqualTo(LocalDateTime.parse("2099-01-01T08:00"));
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
