package com.example.quanlydetai;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogIntegrationTest {
    @Autowired com.example.quanlydetai.service.DeTaiService topics;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PasswordEncoder encoder;
    private static String hash;
    private final Map<String, MockHttpSession> sessions = new HashMap<>();
    private LocalDateTime now;

    @Test
    void fullDraftEditingMovesDepartmentAndSupervisorsAndRequiresNewApproval() throws Exception {
        api("ct.g2", "PATCH", "/api/v1/topics/CT_TOPIC/department-review", "{\"accept\":true}")
                .andExpect(status().isNoContent());
        String body =
                "{\"title\":\"Đề tài AI\",\"content\":\"Yêu cầu"
                        + " mới\",\"department\":\"CT_B2\",\"supervisors\":[\"CT_G1\",\"CT_G3\"]}";
        api("ct.g1", "PUT", "/api/v1/topics/CT_TOPIC", body.replace("CT_G3", "CT_G1"))
                .andExpect(status().isBadRequest());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM DUYET_BO_MON WHERE Ma_De_Tai='CT_TOPIC'",
                                Integer.class))
                .isEqualTo(1);
        api("ct.g1", "PUT", "/api/v1/topics/CT_TOPIC", body).andExpect(status().isNoContent());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Ma_Bo_Mon FROM DE_TAI WHERE Ma_De_Tai='CT_TOPIC'",
                                String.class))
                .isEqualTo("CT_B2");
        assertThat(
                        jdbc.queryForList(
                                "SELECT Ma_GV FROM GVHD_DETAI WHERE Ma_De_Tai='CT_TOPIC'",
                                String.class))
                .containsExactlyInAnyOrder("CT_G1", "CT_G3");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM DUYET_BO_MON WHERE Ma_De_Tai='CT_TOPIC'",
                                Integer.class))
                .isZero();
        api("ct.g2", "PATCH", "/api/v1/topics/CT_TOPIC/department-review", "{\"accept\":true}")
                .andExpect(status().isForbidden());
        api("ct.g3", "PATCH", "/api/v1/topics/CT_TOPIC/department-review", "{\"accept\":true}")
                .andExpect(status().isNoContent());
        api("ct.g0", "PATCH", "/api/v1/topics/CT_TOPIC/review", "{\"accept\":true}")
                .andExpect(status().isNoContent());
        api("ct.g1", "PUT", "/api/v1/topics/CT_TOPIC", body).andExpect(status().isBadRequest());
    }

    @Test
    void reviewClosesWhenStudentRegistrationHasStarted() throws Exception {
        api("ct.g2", "PATCH", "/api/v1/topics/CT_TOPIC/department-review", "{\"accept\":true}")
                .andExpect(status().isNoContent());
        jdbc.update(
                "UPDATE DOT_DANG_KY SET End_GV=?, Begin_SV=? WHERE Ma_Dot='CT_PERIOD'",
                now.minusMinutes(2),
                now.minusMinutes(1));
        em.clear();
        api("ct.g0", "PATCH", "/api/v1/topics/CT_TOPIC/review", "{\"accept\":true}")
                .andExpect(status().isBadRequest());
        api("ct.g2", "PATCH", "/api/v1/topics/CT_TOPIC/department-review", "{\"accept\":true}")
                .andExpect(status().isBadRequest());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Trang_Thai FROM DE_TAI WHERE Ma_De_Tai='CT_TOPIC'",
                                String.class))
                .isEqualTo("Nhap");
    }

    @Test
    void councilsCanBeCorrectedOrDeletedOnlyWhileDraftAndUnused() throws Exception {
        String body =
                "{\"period\":\"CT_PERIOD\",\"chair\":\"CT_G0\",\"secretary\":\"CT_G1\",\"members\":[\"CT_G2\"]}";
        api("ct.g0", "POST", "/api/v1/councils", body).andExpect(status().isCreated());
        String id =
                jdbc.queryForObject(
                        "SELECT Ma_HD FROM HOI_DONG WHERE Ma_Dot='CT_PERIOD'", String.class);
        mvc.perform(get("/dean/councils/" + id + "/edit").session(session("ct.g0")))
                .andExpect(status().isOk());
        api("ct.student", "PUT", "/api/v1/councils/" + id, body).andExpect(status().isForbidden());
        api("ct.g0", "PUT", "/api/v1/councils/" + id, body.replace("CT_G2", "CT_G1"))
                .andExpect(status().isBadRequest());
        String edited =
                "{\"period\":\"CT_PERIOD\",\"chair\":\"CT_G2\",\"secretary\":\"CT_G3\",\"members\":[\"CT_G1\"]}";
        api("ct.g0", "PUT", "/api/v1/councils/" + id, edited).andExpect(status().isNoContent());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Ma_GV FROM THANHVIEN_HOIDONG WHERE Ma_HD=? AND"
                                        + " Vai_Tro='ChuTich'",
                                String.class,
                                id))
                .isEqualTo("CT_G2");
        api("ct.g0", "PATCH", "/api/v1/councils/" + id + "/finalize", "")
                .andExpect(status().isNoContent());
        api("ct.g0", "PUT", "/api/v1/councils/" + id, body).andExpect(status().isBadRequest());
        api("ct.g0", "DELETE", "/api/v1/councils/" + id, "").andExpect(status().isBadRequest());
        api("ct.g0", "POST", "/api/v1/councils", body.replace("CT_PERIOD", "CT_EMPTY"))
                .andExpect(status().isCreated());
        String empty =
                jdbc.queryForObject(
                        "SELECT Ma_HD FROM HOI_DONG WHERE Ma_Dot='CT_EMPTY'", String.class);
        api("ct.g0", "DELETE", "/api/v1/councils/" + empty, "").andExpect(status().isNoContent());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM THANHVIEN_HOIDONG WHERE Ma_HD=?",
                                Integer.class,
                                empty))
                .isZero();
    }

    @Test
    void listingManyTopicsDoesNotAddQueriesPerTopic() throws Exception {
        var context =
                (org.springframework.security.core.context.SecurityContext)
                        session("ct.g0").getAttribute("SPRING_SECURITY_CONTEXT");
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);
        var stats =
                em.getEntityManagerFactory()
                        .unwrap(org.hibernate.SessionFactory.class)
                        .getStatistics();
        boolean enabledBefore = stats.isStatisticsEnabled();
        try {
            stats.setStatisticsEnabled(true);
            em.flush();
            em.clear();
            stats.clear();
            assertThat(topics.all()).hasSize(1);
            long oneTopic = stats.getPrepareStatementCount();
            for (int i = 0; i < 20; i++) {
                String id = "CT_SCALE" + i;
                jdbc.update(
                        "INSERT INTO"
                            + " DE_TAI(Ma_De_Tai,Ten_De_Tai,Noi_Dung,Ma_Bo_Mon,Ma_Dot,Ma_GV_Dang_Ky,Trang_Thai,Ngay_Dang_Ky)"
                            + " VALUES (?,?,'Nội dung','CT_B1','CT_PERIOD','CT_G1','Nhap',?)",
                        id,
                        "Đề tài " + i,
                        now);
                jdbc.update("INSERT INTO GVHD_DETAI(Ma_GV,Ma_De_Tai) VALUES ('CT_G1',?)", id);
            }
            em.clear();
            stats.clear();
            var rows = topics.all();
            long manyTopics = stats.getPrepareStatementCount();
            assertThat(rows)
                    .hasSize(21)
                    .allSatisfy(
                            t -> {
                                assertThat(t.supervisors()).containsExactly("Giảng viên 1");
                                assertThat(t.department()).isEqualTo("CT Công nghệ phần mềm");
                                assertThat(t.periodId()).isEqualTo("CT_PERIOD");
                            });
            assertThat(manyTopics).isEqualTo(oneTopic).isLessThanOrEqualTo(10);
            System.out.println(
                    "Topic list SQL statements: 1 topic=" + oneTopic + ", 21 topics=" + manyTopics);
        } finally {
            stats.setStatisticsEnabled(enabledBefore);
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @BeforeEach
    void seed(TestInfo info) {
        if (hash == null) hash = encoder.encode("Catalog-Test-Password!");
        if (info.getTestMethod()
                .orElseThrow()
                .getName()
                .equals("referencedProfileDeletionRollsBackAccount")) return;
        now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).withNano(0);
        jdbc.update(
                "INSERT INTO BO_MON(Ma_Bo_Mon,Ten_Bo_Mon) VALUES ('CT_B1','CT Công nghệ phần"
                        + " mềm'),('CT_B2','CT Trí tuệ nhân tạo')");
        for (int i = 0; i < 4; i++) {
            String id = "CT_G" + i;
            jdbc.update(
                    "INSERT INTO GV(Ma_GV,Ho_Ten,Email,Ma_Bo_Mon) VALUES (?,?,?,?)",
                    id,
                    "Giảng viên " + i,
                    id + "@example.test",
                    i == 3 ? "CT_B2" : "CT_B1");
            account("ct.g" + i, "GV", id, null);
        }
        jdbc.update("INSERT INTO TRUONG_KHOA(Ma_GV,Dang_Giu_Chuc) VALUES ('CT_G0',true)");
        jdbc.update(
                "INSERT INTO TRUONG_BO_MON(Ma_Bo_Mon,Ma_GV,Dang_Giu_Chuc) VALUES"
                        + " ('CT_B1','CT_G2',true),('CT_B2','CT_G3',true)");
        jdbc.update(
                "INSERT INTO SV(Ma_SV,Ho_Ten,Email) VALUES ('CT_S1','Nguyễn"
                        + " Ánh','student@example.test')");
        account("ct.student", "SV", null, "CT_S1");
        account("ct.admin", "Admin", null, null);
        for (String id : List.of("CT_PERIOD", "CT_EMPTY"))
            jdbc.update(
                    "INSERT INTO"
                        + " DOT_DANG_KY(Ma_Dot,Ma_Truong_Khoa,Ten_Dot_Dang_Ky,Loai,Begin_GV,End_GV,Begin_SV,End_SV)"
                        + " VALUES (?,'CT_G0',?,'MonHoc',?,?,?,?)",
                    id,
                    id,
                    now.minusDays(1),
                    now.plusDays(1),
                    now.plusDays(2),
                    now.plusDays(5));
        jdbc.update(
                "INSERT INTO"
                    + " DE_TAI(Ma_De_Tai,Ten_De_Tai,Noi_Dung,Ma_Bo_Mon,Ma_Dot,Ma_GV_Dang_Ky,Trang_Thai,Ngay_Dang_Ky)"
                    + " VALUES ('CT_TOPIC','Quản lý thư viện','Nội dung đề"
                    + " tài','CT_B1','CT_PERIOD','CT_G1','Nhap',?)",
                now);
        jdbc.update("INSERT INTO GVHD_DETAI(Ma_GV,Ma_De_Tai) VALUES ('CT_G1','CT_TOPIC')");
    }

    private void account(String username, String role, String gv, String sv) {
        jdbc.update(
                "INSERT INTO"
                    + " TAI_KHOAN(Ma_TK,Ten_Dang_Nhap,Mat_Khau_Hash,Vai_Tro,Ma_GV,Ma_SV,Trang_Thai,Ngay_Tao)"
                    + " VALUES (?,?,?,?,?,?,'HoatDong',?)",
                username,
                username,
                hash,
                role,
                gv,
                sv,
                LocalDateTime.now());
    }

    @Test
    @Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void referencedProfileDeletionRollsBackAccount() throws Exception {
        // Run outside the test transaction so the service must roll back its own account delete
        // when deletion of the linked lecturer fails. Fixtures belong only to the test database.
        try {
            jdbc.update(
                    "INSERT INTO BO_MON(Ma_Bo_Mon,Ten_Bo_Mon) VALUES ('CT_DELETE_BM','CT rollback"
                            + " department')");
            jdbc.update(
                    "INSERT INTO GV(Ma_GV,Ho_Ten,Email,Ma_Bo_Mon) VALUES ('CT_DELETE_GV','Rollback"
                            + " teacher','rollback@example.test','CT_DELETE_BM')");
            account("ct.delete.admin", "Admin", null, null);
            account("ct.delete.target", "GV", "CT_DELETE_GV", null);
            jdbc.update(
                    "INSERT INTO TRUONG_BO_MON(Ma_Bo_Mon,Ma_GV,Dang_Giu_Chuc) VALUES"
                            + " ('CT_DELETE_BM','CT_DELETE_GV',false)");
            mvc.perform(
                            delete("/api/v1/admin/accounts/ct.delete.target")
                                    .session(session("ct.delete.admin"))
                                    .with(csrf()))
                    .andExpect(status().isConflict());
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM TAI_KHOAN WHERE Ma_TK='ct.delete.target'",
                                    Integer.class))
                    .isEqualTo(1);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM GV WHERE Ma_GV='CT_DELETE_GV'",
                                    Integer.class))
                    .isEqualTo(1);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM TRUONG_BO_MON WHERE"
                                            + " Ma_Bo_Mon='CT_DELETE_BM'",
                                    Integer.class))
                    .isEqualTo(1);
        } finally {
            jdbc.update("DELETE FROM TRUONG_BO_MON WHERE Ma_Bo_Mon='CT_DELETE_BM'");
            jdbc.update(
                    "DELETE FROM TAI_KHOAN WHERE Ma_TK IN ('ct.delete.admin','ct.delete.target')");
            jdbc.update("DELETE FROM GV WHERE Ma_GV='CT_DELETE_GV'");
            jdbc.update("DELETE FROM BO_MON WHERE Ma_Bo_Mon='CT_DELETE_BM'");
        }
    }

    private MockHttpSession session(String username) throws Exception {
        if (!sessions.containsKey(username))
            sessions.put(
                    username,
                    (MockHttpSession)
                            mvc.perform(
                                            formLogin()
                                                    .user(username)
                                                    .password("Catalog-Test-Password!"))
                                    .andExpect(status().is3xxRedirection())
                                    .andReturn()
                                    .getRequest()
                                    .getSession(false));
        return sessions.get(username);
    }

    private ResultActions api(String user, String method, String url, String body)
            throws Exception {
        var builder =
                switch (method) {
                    case "PUT" -> put(url);
                    case "PATCH" -> patch(url);
                    case "DELETE" -> delete(url);
                    default -> post(url);
                };
        var result =
                mvc.perform(
                        builder.session(session(user))
                                .with(csrf())
                                .contentType("application/json")
                                .content(body));
        em.flush();
        em.clear();
        return result;
    }

    @Test
    void departmentApprovalIsRequiredScopedAndInvalidatedByEditing() throws Exception {
        api("ct.g0", "PATCH", "/api/v1/topics/CT_TOPIC/review", "{\"accept\":true}")
                .andExpect(status().isBadRequest());
        api("ct.g3", "PATCH", "/api/v1/topics/CT_TOPIC/department-review", "{\"accept\":true}")
                .andExpect(status().isForbidden());
        api("ct.g1", "PATCH", "/api/v1/topics/CT_TOPIC/department-review", "{\"accept\":true}")
                .andExpect(status().isForbidden());
        api("ct.g2", "PATCH", "/api/v1/topics/CT_TOPIC/department-review", "{\"accept\":true}")
                .andExpect(status().isNoContent());
        api(
                        "ct.g1",
                        "PUT",
                        "/api/v1/topics/CT_TOPIC",
                        "{\"title\":\"Tên mới\",\"content\":\"Nội dung mới\"}")
                .andExpect(status().isNoContent());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM DUYET_BO_MON WHERE Ma_De_Tai='CT_TOPIC'",
                                Integer.class))
                .isZero();
        api("ct.g0", "PATCH", "/api/v1/topics/CT_TOPIC/review", "{\"accept\":true}")
                .andExpect(status().isBadRequest());
        api("ct.g2", "PATCH", "/api/v1/topics/CT_TOPIC/department-review", "{\"accept\":true}")
                .andExpect(status().isNoContent());
        api("ct.g0", "PATCH", "/api/v1/topics/CT_TOPIC/review", "{\"accept\":true}")
                .andExpect(status().isNoContent());
        api(
                        "ct.g1",
                        "PUT",
                        "/api/v1/topics/CT_TOPIC",
                        "{\"title\":\"Không được sửa\",\"content\":\"Nội dung\"}")
                .andExpect(status().isBadRequest());
        api("ct.g1", "DELETE", "/api/v1/topics/CT_TOPIC", "").andExpect(status().isBadRequest());
    }

    @Test
    void departmentSearchIgnoresAccentsAndDoesNotLimitFormOptions() throws Exception {
        mvc.perform(
                        get("/api/v1/catalog/departments")
                                .session(session("ct.admin"))
                                .param("q", "tri tue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'CT_B2')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id == 'CT_B1')]").isEmpty());
        mvc.perform(
                        get("/admin/accounts")
                                .session(session("ct.admin"))
                                .param("departmentQuery", "CT_B2"))
                .andExpect(status().isOk())
                .andExpect(
                        model().attribute("managedDepartments", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("value=\"CT_B1\"")));
    }

    @Test
    void topicOwnershipValidationCsrfAndDraftDeletion() throws Exception {
        mvc.perform(
                        put("/api/v1/topics/CT_TOPIC")
                                .session(session("ct.g1"))
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(status().isForbidden());
        api(
                        "ct.g3",
                        "PUT",
                        "/api/v1/topics/CT_TOPIC",
                        "{\"title\":\"Other\",\"content\":\"Other\"}")
                .andExpect(status().isForbidden());
        api("ct.g1", "PUT", "/api/v1/topics/CT_TOPIC", "{\"title\":\"\",\"content\":\"\"}")
                .andExpect(status().isBadRequest());
        api("ct.g1", "DELETE", "/api/v1/topics/CT_TOPIC", "").andExpect(status().isNoContent());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM GVHD_DETAI WHERE Ma_De_Tai='CT_TOPIC'",
                                Integer.class))
                .isZero();
    }

    @Test
    void searchRespectsRolesAndVietnameseNames() throws Exception {
        mvc.perform(
                        get("/api/v1/topics/mine")
                                .session(session("ct.g1"))
                                .param("type", "MonHoc")
                                .param("state", "Nhap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/topics/mine").session(session("ct.g1")).param("type", "KLTN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(
                        get("/api/v1/topics/department")
                                .session(session("ct.g3"))
                                .param("type", "MonHoc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(
                        get("/api/v1/topics/mine")
                                .session(session("ct.g1"))
                                .param("q", "quan ly thu vien")
                                .param("department", "CT_B1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/topics/mine").session(session("ct.g3")).param("q", "CT_TOPIC"))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(
                        get("/api/v1/admin/accounts")
                                .session(session("ct.admin"))
                                .param("q", "nguyen anh")
                                .param("role", "SV"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("student@example.test"))
                .andExpect(jsonPath("$[0].matKhauHash").doesNotExist());
        mvc.perform(get("/api/v1/admin/accounts").session(session("ct.student")))
                .andExpect(status().isForbidden());
    }

    @Test
    void profileEditingAndUnusedAccountDeletion() throws Exception {
        api(
                        "ct.admin",
                        "PUT",
                        "/api/v1/admin/accounts/ct.student/profile",
                        "{\"name\":\"Nguyễn Bình\",\"email\":\"binh@example.test\"}")
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT Ho_Ten FROM SV WHERE Ma_SV='CT_S1'", String.class))
                .isEqualTo("Nguyễn Bình");
        api(
                        "ct.student",
                        "PUT",
                        "/api/v1/admin/accounts/ct.student/profile",
                        "{\"name\":\"Other\",\"email\":\"other@example.test\"}")
                .andExpect(status().isForbidden());
        api("ct.admin", "DELETE", "/api/v1/admin/accounts/ct.admin", "")
                .andExpect(status().isBadRequest());
        api("ct.admin", "DELETE", "/api/v1/admin/accounts/ct.student", "")
                .andExpect(status().isNoContent());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM SV WHERE Ma_SV='CT_S1'", Integer.class))
                .isZero();
    }

    @Test
    void personalProfileUsesOnlyAuthenticatedIdentityAndAppearsInSharedNavigation()
            throws Exception {
        mvc.perform(
                        get("/api/v1/session/profile")
                                .param("id", "ct.g2")
                                .session(session("ct.student")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("CT_S1"))
                .andExpect(jsonPath("$.fullName").value("Nguyễn Ánh"))
                .andExpect(jsonPath("$.email").value("student@example.test"))
                .andExpect(jsonPath("$.role").value("SV"))
                .andExpect(jsonPath("$.dean").value(false))
                .andExpect(jsonPath("$.departmentHead").value(false))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.matKhauHash").doesNotExist());
        String studentPage = personalPage("ct.student");
        assertThat(studentPage)
                .contains("Mã số sinh viên", "CT_S1", "Nguyễn Ánh")
                .doesNotContain("Mã số giảng viên", "Chức vụ hiện tại", hash);
        for (String user : List.of("ct.student", "ct.g0", "ct.g1", "ct.g2")) {
            for (String path : List.of("/", "/announcements", "/accounts/password")) {
                mvc.perform(get(path).session(session(user)))
                        .andExpect(status().isOk())
                        .andExpect(
                                content()
                                        .string(
                                                org.hamcrest.Matchers.containsString(
                                                        "href=\"/accounts/profile\"")));
            }
        }
        assertThat(personalPage("ct.g1"))
                .contains("Mã số giảng viên", "CT_G1", "Giảng viên 1", "CT Công nghệ phần mềm")
                .doesNotContain("Trưởng bộ môn CT Công nghệ phần mềm");
        assertThat(personalPage("ct.g2")).contains("Trưởng bộ môn CT Công nghệ phần mềm");
        assertThat(personalPage("ct.g0")).contains("Trưởng khoa Công nghệ thông tin");
        mvc.perform(get("/").session(session("ct.admin")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "href=\"/accounts/profile\""))));
        mvc.perform(get("/accounts/profile").session(session("ct.admin")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/session/profile").session(session("ct.admin")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/accounts/profile")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/v1/session/profile")).andExpect(status().isUnauthorized());
    }

    @Test
    void personalProfileRefreshesAppointmentsInExistingSession() throws Exception {
        var existingSession = session("ct.g1");
        api(
                        "ct.admin",
                        "PUT",
                        "/api/v1/admin/department-head",
                        "{\"department\":\"CT_B1\",\"teacher\":\"CT_G1\",\"appointed\":true}")
                .andExpect(status().isNoContent());
        api("ct.admin", "PUT", "/api/v1/admin/dean", "{\"teacher\":\"CT_G1\",\"appointed\":true}")
                .andExpect(status().isNoContent());
        assertThat(personalPage("ct.g1"))
                .contains("Trưởng khoa Công nghệ thông tin", "Trưởng bộ môn CT Công nghệ phần mềm");
        api(
                        "ct.admin",
                        "PUT",
                        "/api/v1/admin/department-head",
                        "{\"department\":\"CT_B1\",\"teacher\":\"CT_G1\",\"appointed\":false}")
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/session/profile").session(existingSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dean").value(true))
                .andExpect(jsonPath("$.departmentHead").value(false));
        api("ct.admin", "PUT", "/api/v1/admin/dean", "{\"teacher\":\"CT_G1\",\"appointed\":false}")
                .andExpect(status().isNoContent());
        api(
                        "ct.admin",
                        "PUT",
                        "/api/v1/admin/accounts/ct.g1/profile",
                        "{\"name\":\"Tên cập"
                            + " nhật\",\"email\":\"new@example.test\",\"department\":\"CT_B2\"}")
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/session/profile").session(existingSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Tên cập nhật"))
                .andExpect(jsonPath("$.departmentName").value("CT Trí tuệ nhân tạo"))
                .andExpect(jsonPath("$.dean").value(false))
                .andExpect(jsonPath("$.departmentHead").value(false));
        assertThat(personalPage("ct.g1"))
                .contains("Tên cập nhật", "CT Trí tuệ nhân tạo")
                .doesNotContain(
                        "Trưởng bộ môn CT Công nghệ phần mềm", "Trưởng khoa Công nghệ thông tin");
    }

    private String personalPage(String user) throws Exception {
        return mvc.perform(get("/accounts/profile").session(session(user)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    void accountListShowsCurrentAppointmentsWithoutChangingLoginRole() throws Exception {
        accountAppointments("ct.g0", true, false);
        accountAppointments("ct.g1", false, false);
        accountAppointments("ct.g2", false, true);
        accountAppointments("ct.student", false, false);
        accountAppointments("ct.admin", false, false);

        api("ct.admin", "PUT", "/api/v1/admin/dean", "{\"teacher\":\"CT_G2\",\"appointed\":true}")
                .andExpect(status().isNoContent());
        accountAppointments("ct.g2", true, true);
        assertThat(accountPage("ct.g2"))
                .contains(
                        "<span>Giảng viên</span>",
                        "<span class=\"tag\">Trưởng khoa</span>",
                        "<span class=\"tag\">Trưởng bộ môn</span>");
        mvc.perform(
                        get("/api/v1/admin/accounts")
                                .param("q", "ct.g2")
                                .param("role", "GV")
                                .session(session("ct.admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("GV"));

        api("ct.admin", "PUT", "/api/v1/admin/dean", "{\"teacher\":\"CT_G2\",\"appointed\":false}")
                .andExpect(status().isNoContent());
        accountAppointments("ct.g2", false, true);
        api(
                        "ct.admin",
                        "PUT",
                        "/api/v1/admin/department-head",
                        "{\"department\":\"CT_B1\",\"teacher\":\"CT_G2\",\"appointed\":false}")
                .andExpect(status().isNoContent());
        accountAppointments("ct.g2", false, false);
        assertThat(accountPage("ct.g2"))
                .contains("<span>Giảng viên</span>")
                .doesNotContain(
                        "<span class=\"tag\">Trưởng khoa</span>",
                        "<span class=\"tag\">Trưởng bộ môn</span>");

        api(
                        "ct.admin",
                        "PUT",
                        "/api/v1/admin/department-head",
                        "{\"department\":\"CT_B1\",\"teacher\":\"CT_G2\",\"appointed\":true}")
                .andExpect(status().isNoContent());
        api(
                        "ct.admin",
                        "PUT",
                        "/api/v1/admin/department-head",
                        "{\"department\":\"CT_B1\",\"teacher\":\"CT_G1\",\"appointed\":true}")
                .andExpect(status().isNoContent());
        accountAppointments("ct.g2", false, false);
        accountAppointments("ct.g1", false, true);
    }

    private void accountAppointments(String username, boolean dean, boolean departmentHead)
            throws Exception {
        mvc.perform(get("/api/v1/admin/accounts").param("q", username).session(session("ct.admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value(username))
                .andExpect(jsonPath("$[0].dean").value(dean))
                .andExpect(jsonPath("$[0].departmentHead").value(departmentHead));
    }

    private String accountPage(String username) throws Exception {
        return mvc.perform(get("/admin/accounts").param("q", username).session(session("ct.admin")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    void departmentMaintenanceAndRevocationApplyToExistingSession() throws Exception {
        mvc.perform(get("/department/topics").session(session("ct.g2"))).andExpect(status().isOk());
        api(
                        "ct.admin",
                        "PUT",
                        "/api/v1/admin/department-head",
                        "{\"department\":\"CT_B1\",\"teacher\":\"CT_G2\"}")
                .andExpect(status().isBadRequest());
        mvc.perform(get("/department/topics").session(session("ct.g2"))).andExpect(status().isOk());
        api(
                        "ct.admin",
                        "PUT",
                        "/api/v1/admin/accounts/ct.g2/profile",
                        "{\"name\":\"Head\",\"email\":\"head@example.test\",\"department\":\"CT_B2\"}")
                .andExpect(status().isBadRequest());
        api(
                        "ct.admin",
                        "PUT",
                        "/api/v1/admin/department-head",
                        "{\"department\":\"CT_B1\",\"teacher\":\"CT_G3\",\"appointed\":true}")
                .andExpect(status().isBadRequest());
        api(
                        "ct.admin",
                        "PUT",
                        "/api/v1/admin/department-head",
                        "{\"department\":\"CT_B1\",\"teacher\":\"CT_G2\",\"appointed\":false}")
                .andExpect(status().isNoContent());
        mvc.perform(get("/department/topics").session(session("ct.g2")))
                .andExpect(status().isForbidden());
        api("ct.admin", "POST", "/api/v1/admin/departments", "{\"id\":\"CT_NEW\",\"name\":\"New\"}")
                .andExpect(status().isCreated());
        api(
                        "ct.admin",
                        "PUT",
                        "/api/v1/admin/departments/CT_NEW",
                        "{\"id\":\"CT_NEW\",\"name\":\"Updated\"}")
                .andExpect(status().isNoContent());
        api("ct.admin", "DELETE", "/api/v1/admin/departments/CT_NEW", "")
                .andExpect(status().isNoContent());
        api("ct.admin", "DELETE", "/api/v1/admin/departments/CT_B1", "")
                .andExpect(status().isBadRequest());
    }

    @Test
    void noticeScopeOwnershipAndDelete() throws Exception {
        api(
                        "ct.admin",
                        "POST",
                        "/api/v1/announcements",
                        "{\"title\":\"Nhà trường\",\"content\":\"Thông"
                                + " báo\",\"scope\":\"NhaTruong\"}")
                .andExpect(status().isCreated());
        String id =
                jdbc.queryForObject(
                        "SELECT Ma_TB FROM THONG_BAO WHERE Ma_TK='ct.admin'", String.class);
        api(
                        "ct.g0",
                        "PUT",
                        "/api/v1/announcements/" + id,
                        "{\"title\":\"Other\",\"content\":\"Other\",\"scope\":\"Khoa\"}")
                .andExpect(status().isForbidden());
        api(
                        "ct.admin",
                        "PUT",
                        "/api/v1/announcements/" + id,
                        "{\"title\":\"Đã sửa\",\"content\":\"Nội dung mới\",\"scope\":\"Khoa\"}")
                .andExpect(status().isNoContent());
        api("ct.student", "DELETE", "/api/v1/announcements/" + id, "")
                .andExpect(status().isForbidden());
        api("ct.admin", "DELETE", "/api/v1/announcements/" + id, "")
                .andExpect(status().isNoContent());
    }

    @Test
    void periodEditingPreservesOccupiedScheduleAndAllowsEmptyDeletion() throws Exception {
        String body =
                "{\"maDot\":\"CT_PERIOD\",\"tenDotDangKy\":\"Renamed\",\"loai\":\"MonHoc\",\"beginGV\":\""
                        + now.minusDays(1)
                        + "\",\"endGV\":\""
                        + now.plusDays(1)
                        + "\",\"beginSV\":\""
                        + now.plusDays(2)
                        + "\",\"endSV\":\""
                        + now.plusDays(5)
                        + "\"}";
        api("ct.g0", "PUT", "/api/v1/registration-periods/CT_PERIOD", body)
                .andExpect(status().isNoContent());
        api(
                        "ct.g0",
                        "PUT",
                        "/api/v1/registration-periods/CT_PERIOD",
                        body.replace(now.plusDays(1).toString(), now.plusHours(1).toString()))
                .andExpect(status().isBadRequest());
        api("ct.g0", "DELETE", "/api/v1/registration-periods/CT_PERIOD", "")
                .andExpect(status().isBadRequest());
        api("ct.g0", "DELETE", "/api/v1/registration-periods/CT_EMPTY", "")
                .andExpect(status().isNoContent());
        api(
                        "ct.g0",
                        "PUT",
                        "/api/v1/registration-periods/CT_PERIOD",
                        body.replace(now.plusDays(5).toString(), now.plusDays(6).toString()))
                .andExpect(status().isNoContent());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT End_SV FROM DOT_DANG_KY WHERE Ma_Dot='CT_PERIOD'",
                                LocalDateTime.class))
                .isEqualTo(now.plusDays(6));
        api("ct.g0", "PUT", "/api/v1/registration-periods/CT_PERIOD", body)
                .andExpect(status().isBadRequest());
        jdbc.update(
                "UPDATE DOT_DANG_KY SET Loai='TLCN', Deadline_GVPB_Nop_Diem=? WHERE"
                        + " Ma_Dot='CT_PERIOD'",
                now.plusDays(7));
        em.clear();
        String extended =
                body.replace("MonHoc", "TLCN")
                        .replace(now.plusDays(5).toString(), now.plusDays(6).toString())
                        .replace("\"}", "\",\"deadlineGvpbNopDiem\":\"" + now.plusDays(8) + "\"}");
        api("ct.g0", "PUT", "/api/v1/registration-periods/CT_PERIOD", extended)
                .andExpect(status().isNoContent());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Deadline_GVPB_Nop_Diem FROM DOT_DANG_KY WHERE"
                                        + " Ma_Dot='CT_PERIOD'",
                                LocalDateTime.class))
                .isEqualTo(now.plusDays(8));
        api(
                        "ct.g0",
                        "PUT",
                        "/api/v1/registration-periods/CT_PERIOD",
                        extended.replace(now.plusDays(8).toString(), now.plusDays(7).toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void newFormsRenderAndHtmlPostsUseServices() throws Exception {
        for (String path : List.of("/admin/accounts", "/announcements"))
            mvc.perform(get(path).session(session("ct.admin"))).andExpect(status().isOk());
        for (String path :
                List.of("/dean/periods/CT_PERIOD/edit", "/dean/approvals", "/lecturer/topics"))
            mvc.perform(get(path).session(session("ct.g0"))).andExpect(status().isOk());
        mvc.perform(get("/student/topics").session(session("ct.student")))
                .andExpect(status().isOk());
        mvc.perform(
                        post("/lecturer/topics/CT_TOPIC/edit")
                                .session(session("ct.g1"))
                                .with(csrf())
                                .param("title", "Tên qua form")
                                .param("content", "Nội dung qua form"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("thanhCong"));
        em.flush();
        em.clear();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Ten_De_Tai FROM DE_TAI WHERE Ma_De_Tai='CT_TOPIC'",
                                String.class))
                .isEqualTo("Tên qua form");
    }

    @Test
    void loggingCapturesOutcomeWithoutPasswordsOrFormContents() throws Exception {
        var logger =
                (ch.qos.logback.classic.Logger)
                        org.slf4j.LoggerFactory.getLogger(
                                com.example.quanlydetai.audit.LoggingAspect.class);
        var appender =
                new ch.qos.logback.core.read.ListAppender<
                        ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            api(
                            "ct.admin",
                            "PUT",
                            "/api/v1/admin/accounts/ct.student/password",
                            "{\"password\":\"New-Private-Test-Password!\"}")
                    .andExpect(status().isNoContent());
            api(
                            "ct.admin",
                            "PUT",
                            "/api/v1/admin/departments/MISSING",
                            "{\"id\":\"MISSING\",\"name\":\"Secret-Form-Content\"}")
                    .andExpect(status().isBadRequest());
            String logs =
                    appender.list.stream()
                            .map(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage)
                            .collect(java.util.stream.Collectors.joining("\n"));
            assertThat(logs)
                    .contains("outcome=SUCCESS", "outcome=FAILED:BusinessException", "durationMs=")
                    .doesNotContain(
                            "New-Private-Test-Password!",
                            "Secret-Form-Content",
                            hash,
                            "Catalog-Test-Password!");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
