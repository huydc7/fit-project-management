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
    void supervisionLimitCountsProposedDraftsAndKeepsPeriodsIndependent() throws Exception {
        for (int i = 1; i <= 2; i++) {
            String body = "{\"period\":\"CT_PERIOD\",\"title\":\"Auto " + i + "\",\"content\":\"Quota\",\"department\":\"CT_B1\"}";
            api("ct.g1", "POST", "/api/v1/topics", body).andExpect(status().isCreated());
            String id = jdbc.queryForObject("SELECT Ma_De_Tai FROM DE_TAI WHERE Ten_De_Tai=?", String.class, "Auto " + i);
            api("ct.g2", "PATCH", "/api/v1/topics/" + id + "/department-review", "{\"accept\":true}")
                    .andExpect(status().isNoContent());
            assertThat(jdbc.queryForList("SELECT Ma_GV FROM GVHD_DETAI WHERE Ma_De_Tai=?", String.class, id)).containsExactly("CT_G1");
        }
        api("ct.g1", "POST", "/api/v1/topics",
                "{\"period\":\"CT_PERIOD\",\"title\":\"Fourth\",\"content\":\"Quota\",\"department\":\"CT_B1\"}")
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM DE_TAI WHERE Ten_De_Tai='Fourth'", Integer.class)).isZero();
        api("ct.g1", "POST", "/api/v1/topics",
                "{\"period\":\"CT_EMPTY\",\"title\":\"Other period\",\"content\":\"Quota\",\"department\":\"CT_B1\"}")
                .andExpect(status().isCreated());
    }

    @Test
    void cancelledTopicReleasesSupervisionSlotButPublishedTopicStillCounts() throws Exception {
        for (int i = 1; i <= 2; i++) {
            quotaTopic("CT_Q" + i, "CT_PERIOD");
            api("ct.g2", "PATCH", "/api/v1/topics/CT_Q" + i + "/department-review",
                    "{\"accept\":true}")
                    .andExpect(status().isNoContent());
        }
        api("ct.g0", "PATCH", "/api/v1/topics/CT_Q1/review", "{\"accept\":true}").andExpect(status().isNoContent());
        quotaTopic("CT_Q4", "CT_PERIOD");
        mvc.perform(post("/department/topics/CT_Q4/review").session(session("ct.g2")).with(csrf())
                        .param("accept", "true"))
                .andExpect(flash().attribute("loi", org.hamcrest.Matchers.containsString("đủ 3 đề tài")));
        api("ct.g0", "PATCH", "/api/v1/topics/CT_Q2/review", "{\"accept\":false}").andExpect(status().isNoContent());
        api("ct.g2", "PATCH", "/api/v1/topics/CT_Q4/department-review",
                "{\"accept\":true}").andExpect(status().isNoContent());
    }

    @Test
    void topicManagementScopesFacultyDepartmentAndPersonalLists() throws Exception {
        jdbc.update("INSERT INTO DE_TAI(Ma_De_Tai,Ten_De_Tai,Noi_Dung,Ma_Bo_Mon,Ma_Dot,Ma_GV_Dang_Ky,Trang_Thai,Ngay_Dang_Ky)"
                        + " VALUES ('CT_MANAGE_OTHER','Other department topic','Other content','CT_B2','CT_EMPTY','CT_G3','Nhap',?)", now);
        mvc.perform(get("/dean/topics").session(session("ct.g0")))
                .andExpect(status().isOk()).andExpect(view().name("topics/management"))
                .andExpect(model().attribute("topics", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CT_MANAGE_OTHER")));
        mvc.perform(get("/department/topic-management").session(session("ct.g2")))
                .andExpect(status().isOk()).andExpect(model().attribute("topics", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("CT_MANAGE_OTHER"))));
        mvc.perform(get("/department/topic-management").param("department", "CT_B2").session(session("ct.g2")))
                .andExpect(status().isOk()).andExpect(model().attribute("topics", org.hamcrest.Matchers.empty()));
        mvc.perform(get("/lecturer/topic-management").session(session("ct.g1")))
                .andExpect(status().isOk()).andExpect(model().attribute("topics", org.hamcrest.Matchers.hasSize(1)));
        mvc.perform(get("/lecturer/topic-management").param("period", "CT_EMPTY").session(session("ct.g1")))
                .andExpect(status().isOk()).andExpect(model().attribute("topics", org.hamcrest.Matchers.empty()));
        for (String path : List.of("/dean/topics", "/department/topic-management"))
            mvc.perform(get(path).session(session("ct.g1"))).andExpect(status().isForbidden());
        for (String user : List.of("ct.student", "ct.admin"))
            mvc.perform(get("/lecturer/topic-management").session(session(user))).andExpect(status().isForbidden());
    }

    @Test
    void managementFiltersAndPaginationRetainSelectedPeriodAndOtherFilters() throws Exception {
        quotaTopic("CT_NEXT", "CT_PERIOD");
        var result = mvc.perform(get("/dean/topics").session(session("ct.g0"))
                        .param("q", "CT_").param("period", "CT_PERIOD").param("department", "CT_B1")
                        .param("type", "MonHoc").param("state", "Nhap").param("size", "1"))
                .andExpect(status().isOk()).andExpect(model().attribute("topics", org.hamcrest.Matchers.hasSize(1)))
                .andReturn();
        String html = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var nextLink = java.util.regex.Pattern.compile("href=\"([^\"]+)\">Trang sau</a>").matcher(html);
        assertThat(nextLink.find()).isTrue();
        String next = nextLink.group(1).replace("&amp;", "&");
        assertThat(next).contains("/dean/topics?", "page=1", "period=CT_PERIOD", "department=CT_B1", "type=MonHoc", "state=Nhap");
        mvc.perform(get(java.net.URI.create(next)).session(session("ct.g0"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CT_TOPIC")));
    }

    @Test
    void sharedAccountMenuShowsFreshEscapedProfileNameAndRoleSpecificManagementLink() throws Exception {
        var teacherSession = session("ct.g1");
        mvc.perform(get("/announcements").session(teacherSession))
                .andExpect(model().attribute("accountDisplayName", "Giảng viên 1"))
                .andExpect(model().attribute("topicManagementPath", "/lecturer/topic-management"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("account-chevron")));
        jdbc.update("UPDATE GV SET Ho_Ten=? WHERE Ma_GV='CT_G1'", "Nguyễn <script>alert(1)</script>");
        em.clear();
        mvc.perform(get("/announcements").session(teacherSession))
                .andExpect(model().attribute("accountDisplayName", "Nguyễn <script>alert(1)</script>"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn &lt;script&gt;")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Nguyễn <script>"))));
        mvc.perform(get("/announcements").session(session("ct.student")))
                .andExpect(model().attribute("accountDisplayName", "Nguyễn Ánh"));
        mvc.perform(get("/announcements").session(session("ct.g0")))
                .andExpect(model().attribute("topicManagementPath", "/dean/topics"));
        mvc.perform(get("/announcements").session(session("ct.g2")))
                .andExpect(model().attribute("topicManagementPath", "/department/topic-management"));
        mvc.perform(get("/announcements").session(session("ct.admin")))
                .andExpect(model().attribute("accountDisplayName", "Quản trị viên"));
    }

    @Test
    void proposalPageOnlyCreatesWhileManagementProvidesProtectedEditAndDelete() throws Exception {
        mvc.perform(get("/lecturer/topics").session(session("ct.g1")))
                .andExpect(status().isOk()).andExpect(model().attributeDoesNotExist("topics", "topicsPage"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("CT_TOPIC"))));
        mvc.perform(get("/lecturer/topic-management").session(session("ct.g1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"topic-detail-CT_TOPIC\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-controls=\"topic-detail-CT_TOPIC\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đóng chi tiết đề tài")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/lecturer/topics/CT_TOPIC/edit\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/lecturer/topics/CT_TOPIC/delete\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">Tìm kiếm</button>")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Áp dụng bộ lọc"))));
        mvc.perform(get("/lecturer/topics/CT_TOPIC/edit").session(session("ct.g1")))
                .andExpect(status().isOk()).andExpect(view().name("gv/topic-edit"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lưu thay đổi")));
        mvc.perform(get("/lecturer/topics/CT_TOPIC/edit").session(session("ct.g3")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/lecturer/topics/CT_TOPIC/edit").session(session("ct.g1")).with(csrf())
                        .param("title", "Updated from management").param("content", "Updated content"))
                .andExpect(redirectedUrl("/lecturer/topic-management"))
                .andExpect(flash().attributeExists("thanhCong"));
        mvc.perform(post("/lecturer/topics/CT_TOPIC/delete").session(session("ct.g1")).with(csrf()))
                .andExpect(redirectedUrl("/lecturer/topic-management"))
                .andExpect(flash().attributeExists("thanhCong"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM DE_TAI WHERE Ma_De_Tai='CT_TOPIC'", Integer.class)).isZero();
    }

    @Test
    void approvalQueueExcludesUnreviewedAndProcessedTopicsEvenWithForgedStateFilter() throws Exception {
        // A proposal remains in the department queue until its professional review is complete.
        mvc.perform(get("/dean/approvals").session(session("ct.g0")))
                .andExpect(status().isOk()).andExpect(model().attribute("topics", org.hamcrest.Matchers.empty()));
        api("ct.g2", "PATCH", "/api/v1/topics/CT_TOPIC/department-review", "{\"accept\":true}")
                .andExpect(status().isNoContent());
        mvc.perform(get("/dean/approvals").param("state", "Huy").session(session("ct.g0")))
                .andExpect(status().isOk()).andExpect(model().attribute("topics", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/dean/topics/CT_TOPIC/review")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("name=\"state\""))));
        mvc.perform(get("/dean/approvals").param("period", "CT_EMPTY").session(session("ct.g0")))
                .andExpect(model().attribute("topics", org.hamcrest.Matchers.empty()));
        mvc.perform(get("/dean/approvals").session(session("ct.g1"))).andExpect(status().isForbidden());
        api("ct.g0", "PATCH", "/api/v1/topics/CT_TOPIC/review", "{\"accept\":true}")
                .andExpect(status().isNoContent());
        mvc.perform(get("/dean/approvals").param("state", "DaCongBo").session(session("ct.g0")))
                .andExpect(model().attribute("topics", org.hamcrest.Matchers.empty()));
        mvc.perform(get("/dean/topics").session(session("ct.g0")))
                .andExpect(model().attribute("topics", org.hamcrest.Matchers.hasSize(1)));
        quotaTopic("CT_REJECTED", "CT_PERIOD");
        api("ct.g2", "PATCH", "/api/v1/topics/CT_REJECTED/department-review", "{\"accept\":true}")
                .andExpect(status().isNoContent());
        api("ct.g0", "PATCH", "/api/v1/topics/CT_REJECTED/review", "{\"accept\":false}")
                .andExpect(status().isNoContent());
        mvc.perform(get("/dean/approvals").param("state", "Huy").session(session("ct.g0")))
                .andExpect(model().attribute("topics", org.hamcrest.Matchers.empty()));
    }

    private void quotaTopic(String id, String period) {
        jdbc.update("INSERT INTO DE_TAI(Ma_De_Tai,Ten_De_Tai,Noi_Dung,Ma_Bo_Mon,Ma_Dot,Ma_GV_Dang_Ky,Trang_Thai,Ngay_Dang_Ky)"
                + " VALUES (?,?,'Quota test','CT_B1',?,'CT_G1','Nhap',?)", id, id, period, now);
    }

    @Test
    void assignmentOptionsExcludeLockedAndAccountlessLecturers() throws Exception {
        unavailableLecturers();
        mvc.perform(get("/api/v1/catalog/lecturers").session(session("ct.g1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'CT_G1')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id == 'CT_G3')]").isEmpty())
                .andExpect(jsonPath("$[?(@.id == 'CT_ORPHAN')]").isEmpty());
        mvc.perform(get("/department/topics").session(session("ct.g2")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "value=\"CT_G3\""))));
        // Admin can still select a locked lecturer when revoking an appointment.
        mvc.perform(get("/admin/accounts").session(session("ct.admin")))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("value=\"CT_G3\"")));
    }


    @Test
    void departmentReviewFormOnlyApprovesContentWithoutSupervisorSelection() throws Exception {
        mvc.perform(get("/department/topics").session(session("ct.g2")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("name=\"supervisors\""))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Duyệt chuyên môn và chuyển Trưởng khoa")));
        // The faculty-wide catalogue remains available for the Dean's reviewer/council assignments.
        mvc.perform(get("/api/v1/catalog/lecturers").session(session("ct.g0")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == 'CT_G3')]").isNotEmpty());
    }


    @Test
    void departmentRejectsAnyForgedSupervisorSelectionAndPreservesTheProposer() throws Exception {
        for (String selection : List.of("[]", "[\"CT_G1\"]", "[\"CT_G2\"]", "[\"CT_G1\",\"CT_G2\"]", "[\"CT_G3\"]")) {
            api("ct.g2", "PATCH", "/api/v1/topics/CT_TOPIC/department-review",
                    "{\"accept\":true,\"supervisors\":" + selection + "}")
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.supervisors").exists());
            assertThat(jdbc.queryForList("SELECT Ma_GV FROM GVHD_DETAI WHERE Ma_De_Tai='CT_TOPIC'", String.class)).containsExactly("CT_G1");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM DUYET_BO_MON WHERE Ma_De_Tai='CT_TOPIC'", Integer.class)).isZero();
        }
        mvc.perform(post("/department/topics/CT_TOPIC/review").session(session("ct.g2")).with(csrf())
                        .param("accept", "true").param("supervisors", "CT_G1", "CT_G2"))
                .andExpect(redirectedUrl("/department/topics")).andExpect(flash().attributeExists("loi"));
        assertThat(jdbc.queryForList("SELECT Ma_GV FROM GVHD_DETAI WHERE Ma_De_Tai='CT_TOPIC'", String.class)).containsExactly("CT_G1");
        // Old unapproved drafts may still contain a co-supervisor. Review normalizes
        // this draft to the proposer without offering a selection to the department head.
        jdbc.update("INSERT INTO GVHD_DETAI(Ma_GV,Ma_De_Tai) VALUES ('CT_G2','CT_TOPIC')");
        mvc.perform(get("/lecturer/topic-management").session(session("ct.g1")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Giảng viên 1, Giảng viên 2")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("quy trình cũ")));
        api("ct.g2", "PATCH", "/api/v1/topics/CT_TOPIC/department-review", "{\"accept\":true}")
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForList("SELECT Ma_GV FROM GVHD_DETAI WHERE Ma_De_Tai='CT_TOPIC'", String.class)).containsExactly("CT_G1");
    }

    @Test
    void deanCanStillAssignReviewerFromAnotherDepartment() throws Exception {
        api(
                        "ct.g2",
                        "PATCH",
                        "/api/v1/topics/CT_TOPIC/department-review",
                        "{\"accept\":true}")
                .andExpect(status().isNoContent());
        api("ct.g0", "PATCH", "/api/v1/topics/CT_TOPIC/review", "{\"accept\":true}")
                .andExpect(status().isNoContent());
        // Advance the fixture into student registration only after publication. MySQL also
        // requires an approved group before the topic can enter DangThucHien.
        jdbc.update(
                "UPDATE DOT_DANG_KY SET End_GV=?, Begin_SV=? WHERE Ma_Dot='CT_PERIOD'",
                now.minusHours(2),
                now.minusHours(1));
        em.clear();
        api(
                        "ct.student",
                        "POST",
                        "/api/v1/groups",
                        "{\"period\":\"CT_PERIOD\",\"name\":\"Cross-department reviewer group\"}")
                .andExpect(status().isCreated());
        String group =
                jdbc.queryForObject(
                        "SELECT Ma_Nhom FROM THANH_VIEN_NHOM WHERE Ma_SV='CT_S1'", String.class);
        for (int i = 2; i <= 3; i++) {
            String student = "CT_S" + i;
            String username = "ct.student" + i;
            jdbc.update(
                    "INSERT INTO SV(Ma_SV,Ho_Ten,Email) VALUES (?,?,?)",
                    student,
                    "Student " + i,
                    student + "@example.test");
            account(username, "SV", null, student);
            api(
                            "ct.student",
                            "POST",
                            "/api/v1/groups/" + group + "/invitations",
                            "{\"student\":\"" + student + "\"}")
                    .andExpect(status().isCreated());
            String invitation =
                    jdbc.queryForObject(
                            "SELECT Ma_Loi_Moi FROM LOI_MOI_NHOM WHERE Ma_Nhom=? AND Ma_SV=?",
                            String.class,
                            group,
                            student);
            api(username, "PATCH", "/api/v1/invitations/" + invitation, "{\"accept\":true}")
                    .andExpect(status().isNoContent());
        }
        api(
                        "ct.student",
                        "POST",
                        "/api/v1/registrations",
                        "{\"group\":\"" + group + "\",\"topic\":\"CT_TOPIC\"}")
                .andExpect(status().isCreated());
        String registration =
                jdbc.queryForObject(
                        "SELECT Ma_DK FROM DANG_KY_DE_TAI WHERE Ma_Nhom=?", String.class, group);
        api("ct.g1", "PATCH", "/api/v1/registrations/" + registration + "/review", "{\"accept\":true}")
                .andExpect(status().isNoContent());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Trang_Thai FROM DE_TAI WHERE Ma_De_Tai='CT_TOPIC'",
                                String.class))
                .isEqualTo("DangThucHien");
        api("ct.g0", "PUT", "/api/v1/councils/topics/CT_TOPIC/reviewer", "{\"teacher\":\"CT_G3\"}")
                .andExpect(status().isNoContent());
        assertThat(
                        jdbc.queryForList(
                                "SELECT Ma_GV FROM GVPB_DETAI WHERE Ma_De_Tai='CT_TOPIC'",
                                String.class))
                .containsExactly("CT_G3");
    }


    @Test
    void departmentApprovalRechecksProposerAccountWithoutChangingSupervisor() throws Exception {
        jdbc.update("UPDATE TAI_KHOAN SET Trang_Thai='Khoa' WHERE Ma_TK='ct.g1'");
        em.clear();
        api("ct.g2", "PATCH", "/api/v1/topics/CT_TOPIC/department-review", "{\"accept\":true}")
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForList("SELECT Ma_GV FROM GVHD_DETAI WHERE Ma_De_Tai='CT_TOPIC'", String.class)).containsExactly("CT_G1");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM DUYET_BO_MON WHERE Ma_De_Tai='CT_TOPIC'", Integer.class)).isZero();
    }

    @Test
    void publicationRechecksSupervisorAccountAfterDepartmentApproval() throws Exception {
        api(
                        "ct.g2",
                        "PATCH",
                        "/api/v1/topics/CT_TOPIC/department-review",
                        "{\"accept\":true}")
                .andExpect(status().isNoContent());
        api(
                        "ct.admin",
                        "PATCH",
                        "/api/v1/admin/accounts/ct.g1/status",
                        "{\"active\":false,\"confirmPendingTasks\":true}")
                .andExpect(status().isNoContent());
        api("ct.g0", "PATCH", "/api/v1/topics/CT_TOPIC/review", "{\"accept\":true}")
                .andExpect(status().isBadRequest());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Trang_Thai FROM DE_TAI WHERE Ma_De_Tai='CT_TOPIC'",
                                String.class))
                .isEqualTo("Nhap");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Ma_GV_Xac_Nhan FROM DE_TAI WHERE Ma_De_Tai='CT_TOPIC'",
                                String.class))
                .isNull();
    }

    @Test
    void councilCreationEditingAndFinalizationCheckMemberAccounts() throws Exception {
        unavailableLecturers();
        String body =
                "{\"period\":\"CT_PERIOD\",\"chair\":\"CT_G0\",\"secretary\":\"CT_G1\",\"members\":[\"CT_G2\"]}";
        for (String teacher : List.of("CT_G3", "CT_ORPHAN")) {
            api("ct.g0", "POST", "/api/v1/councils", body.replace("CT_G2", teacher))
                    .andExpect(status().isBadRequest());
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM HOI_DONG WHERE Ma_Dot='CT_PERIOD'",
                                    Integer.class))
                    .isZero();
        }
        api("ct.g0", "POST", "/api/v1/councils", body).andExpect(status().isCreated());
        String id =
                jdbc.queryForObject(
                        "SELECT Ma_HD FROM HOI_DONG WHERE Ma_Dot='CT_PERIOD'", String.class);
        api("ct.g0", "PUT", "/api/v1/councils/" + id, body.replace("CT_G2", "CT_ORPHAN"))
                .andExpect(status().isBadRequest());
        assertThat(
                        jdbc.queryForList(
                                "SELECT Ma_GV FROM THANHVIEN_HOIDONG WHERE Ma_HD=?",
                                String.class,
                                id))
                .containsExactlyInAnyOrder("CT_G0", "CT_G1", "CT_G2");
        jdbc.update("UPDATE TAI_KHOAN SET Trang_Thai='Khoa' WHERE Ma_TK='ct.g2'");
        em.clear();
        api("ct.g0", "PATCH", "/api/v1/councils/" + id + "/finalize", "")
                .andExpect(status().isBadRequest());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Trang_Thai FROM HOI_DONG WHERE Ma_HD=?", String.class, id))
                .isEqualTo("DangLap");
    }

    @Test
    void lockingShowsPendingTasksRequiresConfirmationAndPreservesAssignments() throws Exception {
        var oldSession = session("ct.g1");
        String impact = "/api/v1/admin/accounts/ct.g1/lock-impact";
        mvc.perform(get(impact).session(session("ct.admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks[0].role").value("GVHD"))
                .andExpect(jsonPath("$.tasks[0].topicId").value("CT_TOPIC"))
                .andExpect(jsonPath("$.matKhauHash").doesNotExist());
        mvc.perform(get(impact).session(oldSession)).andExpect(status().isForbidden());
        mvc.perform(get(impact)).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/accounts/ct.g1/lock").session(session("ct.admin")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "confirmPendingTasks")));
        api("ct.admin", "PATCH", "/api/v1/admin/accounts/ct.g1/status", "{\"active\":false}")
                .andExpect(status().isBadRequest());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Trang_Thai FROM TAI_KHOAN WHERE Ma_TK='ct.g1'",
                                String.class))
                .isEqualTo("HoatDong");
        mvc.perform(
                        post("/admin/accounts/ct.g1/status")
                                .session(session("ct.admin"))
                                .with(csrf())
                                .param("active", "false"))
                .andExpect(redirectedUrl("/admin/accounts/ct.g1/lock"))
                .andExpect(flash().attributeExists("loi"));
        mvc.perform(
                        post("/admin/accounts/ct.g1/status")
                                .session(session("ct.admin"))
                                .with(csrf())
                                .param("active", "false")
                                .param("confirmPendingTasks", "true"))
                .andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attributeExists("thanhCong"));
        assertThat(
                        jdbc.queryForList(
                                "SELECT Ma_GV FROM GVHD_DETAI WHERE Ma_De_Tai='CT_TOPIC'",
                                String.class))
                .containsExactly("CT_G1");
        mvc.perform(get("/").session(oldSession)).andExpect(redirectedUrl("/login?expired"));
        api("ct.admin", "PATCH", "/api/v1/admin/accounts/ct.g1/status", "{\"active\":true}")
                .andExpect(status().isNoContent());
    }

    @Test
    void lecturerWithoutPendingTasksCanBeLockedWithoutExtraConfirmation() throws Exception {
        api("ct.admin", "PATCH", "/api/v1/admin/accounts/ct.g3/status", "{\"active\":false}")
                .andExpect(status().isNoContent());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Trang_Thai FROM TAI_KHOAN WHERE Ma_TK='ct.g3'",
                                String.class))
                .isEqualTo("Khoa");
    }

    private void unavailableLecturers() {
        jdbc.update("UPDATE TAI_KHOAN SET Trang_Thai='Khoa' WHERE Ma_TK='ct.g3'");
        jdbc.update(
                "INSERT INTO GV(Ma_GV,Ho_Ten,Email,Ma_Bo_Mon) VALUES ('CT_ORPHAN','Chưa có tài"
                        + " khoản','orphan@example.test','CT_B1')");
        em.clear();
    }


    @Test
    void draftEditingKeepsProposerAsSupervisorAndRequiresNewDepartmentApproval() throws Exception {
        api("ct.g2", "PATCH", "/api/v1/topics/CT_TOPIC/department-review",
                "{\"accept\":true}")
                .andExpect(status().isNoContent());
        String body = "{\"title\":\"Đề tài mới\",\"content\":\"Yêu cầu mới\",\"department\":\"CT_B1\"}";
        api("ct.g1", "PUT", "/api/v1/topics/CT_TOPIC", body.replace("}", ",\"supervisors\":[\"CT_G2\"]}"))
                .andExpect(status().isBadRequest());
        api("ct.g1", "PUT", "/api/v1/topics/CT_TOPIC", body.replace("CT_B1", "CT_B2"))
                .andExpect(status().isBadRequest());
        api("ct.g1", "PUT", "/api/v1/topics/CT_TOPIC", body).andExpect(status().isNoContent());
        assertThat(jdbc.queryForList("SELECT Ma_GV FROM GVHD_DETAI WHERE Ma_De_Tai='CT_TOPIC'", String.class))
                .containsExactly("CT_G1");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM DUYET_BO_MON WHERE Ma_De_Tai='CT_TOPIC'", Integer.class)).isZero();
        api("ct.g0", "PATCH", "/api/v1/topics/CT_TOPIC/review", "{\"accept\":true}").andExpect(status().isBadRequest());
        api("ct.g2", "PATCH", "/api/v1/topics/CT_TOPIC/department-review",
                "{\"accept\":true,\"supervisors\":[\"CT_G2\"]}").andExpect(status().isBadRequest());
        api("ct.g2", "PATCH", "/api/v1/topics/CT_TOPIC/department-review",
                "{\"accept\":true}").andExpect(status().isNoContent());
        api("ct.g0", "PATCH", "/api/v1/topics/CT_TOPIC/review", "{\"accept\":true}").andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT Ma_GV_Xac_Nhan FROM DE_TAI WHERE Ma_De_Tai='CT_TOPIC'", String.class))
                .isEqualTo("CT_G0");
        api("ct.g1", "PUT", "/api/v1/topics/CT_TOPIC", body).andExpect(status().isBadRequest());
    }

    @Test
    void proposalAutomaticallyAssignsCurrentTeacherAndRejectsForgedSupervisors() throws Exception {
        String proposal =
                "{\"period\":\"CT_PERIOD\",\"title\":\"Đề xuất mới\",\"content\":\"Nội"
                        + " dung\",\"department\":\"CT_B1\"}";
        api("ct.g1", "POST", "/api/v1/topics", proposal).andExpect(status().isCreated());
        String id =
                jdbc.queryForObject(
                        "SELECT Ma_De_Tai FROM DE_TAI WHERE Ten_De_Tai='Đề xuất mới'",
                        String.class);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM GVHD_DETAI WHERE Ma_De_Tai=?",
                                Integer.class,
                                id))
                .isEqualTo(1);
        api(
                        "ct.g1",
                        "POST",
                        "/api/v1/topics",
                        proposal.replace("}", ",\"supervisors\":[\"CT_G1\"]}"))
                .andExpect(status().isBadRequest());
        for (String payload :
                List.of(
                        "{\"accept\":true,\"supervisors\":[]}",
                        "{\"accept\":true,\"supervisors\":[\"CT_G1\",\"CT_G1\"]}",
                        "{\"accept\":true,\"supervisors\":[\"CT_G1\",\"CT_G2\",\"CT_G3\"]}",
                        "{\"accept\":true,\"supervisors\":[\"CT_G2\",\"UNKNOWN\"]}")) {
            api("ct.g2", "PATCH", "/api/v1/topics/" + id + "/department-review", payload)
                    .andExpect(status().isBadRequest());
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM DUYET_BO_MON WHERE Ma_De_Tai=?",
                                    Integer.class,
                                    id))
                    .isZero();
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM GVHD_DETAI WHERE Ma_De_Tai=?",
                                    Integer.class,
                                    id))
                    .isEqualTo(1);
        }
        api(
                        "ct.g2",
                        "PATCH",
                        "/api/v1/topics/" + id + "/department-review",
                        "{\"accept\":true}")
                .andExpect(status().isNoContent());
        assertThat(
                        jdbc.queryForList(
                                "SELECT Ma_GV FROM GVHD_DETAI WHERE Ma_De_Tai=?", String.class, id))
                .containsExactly("CT_G1");
    }

    @Test
    void htmlProposalAssignsLoggedInTeacherWithoutSupervisorInput() throws Exception {
        mvc.perform(get("/lecturer/topics").session(session("ct.g1")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("name=\"supervisors\""))));
        mvc.perform(post("/lecturer/topics").session(session("ct.g1")).with(csrf())
                        .param("period", "CT_PERIOD").param("title", "HTML automatic supervisor")
                        .param("content", "New proposal").param("department", "CT_B1"))
                .andExpect(redirectedUrl("/lecturer/topics"))
                .andExpect(flash().attributeExists("thanhCong"));
        assertThat(jdbc.queryForList("SELECT h.Ma_GV FROM GVHD_DETAI h JOIN DE_TAI t ON t.Ma_De_Tai=h.Ma_De_Tai WHERE t.Ten_De_Tai='HTML automatic supervisor'", String.class))
                .containsExactly("CT_G1");
        mvc.perform(post("/lecturer/topics").session(session("ct.g1")).with(csrf())
                        .param("period", "CT_PERIOD").param("title", "Forged supervisor")
                        .param("content", "New proposal").param("department", "CT_B1").param("supervisors", "CT_G2"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "supervisors"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM DE_TAI WHERE Ten_De_Tai='Forged supervisor'", Integer.class)).isZero();
    }

    @Test
    void htmlDepartmentApprovalNeedsOnlyDecisionAndKeepsProposer() throws Exception {
        mvc.perform(get("/department/topics").session(session("ct.g2")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(
                                                "name=\"supervisors\""))));
        mvc.perform(
                        post("/department/topics/CT_TOPIC/review")
                                .session(session("ct.g2"))
                                .with(csrf())
                                .param("accept", "true"))
                .andExpect(status().is3xxRedirection());
        em.flush();
        em.clear();
        assertThat(
                        jdbc.queryForList(
                                "SELECT Ma_GV FROM GVHD_DETAI WHERE Ma_De_Tai='CT_TOPIC'",
                                String.class))
                .containsExactly("CT_G1");
    }

    @Test
    void deanAndDepartmentCanRejectAnUnassignedProposal() throws Exception {
        jdbc.update("DELETE FROM GVHD_DETAI WHERE Ma_De_Tai='CT_TOPIC'");
        api("ct.g0", "PATCH", "/api/v1/topics/CT_TOPIC/review", "{\"accept\":false}")
                .andExpect(status().isNoContent());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Trang_Thai FROM DE_TAI WHERE Ma_De_Tai='CT_TOPIC'",
                                String.class))
                .isEqualTo("Huy");
    }

    @Test
    void reviewClosesWhenStudentRegistrationHasStarted() throws Exception {
        api(
                        "ct.g2",
                        "PATCH",
                        "/api/v1/topics/CT_TOPIC/department-review",
                        "{\"accept\":true}")
                .andExpect(status().isNoContent());
        jdbc.update(
                "UPDATE DOT_DANG_KY SET End_GV=?, Begin_SV=? WHERE Ma_Dot='CT_PERIOD'",
                now.minusMinutes(2),
                now.minusMinutes(1));
        em.clear();
        api("ct.g0", "PATCH", "/api/v1/topics/CT_TOPIC/review", "{\"accept\":true}")
                .andExpect(status().isBadRequest());
        api(
                        "ct.g2",
                        "PATCH",
                        "/api/v1/topics/CT_TOPIC/department-review",
                        "{\"accept\":true}")
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

    @Test
    void databaseSearchAppliesAllTopicFiltersBeforePagingAndLimitsEntityLoads() throws Exception {
        for (int i = 0; i < 42; i++) {
            String id = "CT_Q" + String.format("%02d", i);
            jdbc.update(
                    "INSERT INTO"
                        + " DE_TAI(Ma_De_Tai,Ten_De_Tai,Noi_Dung,Ma_Bo_Mon,Ma_Dot,Ma_GV_Dang_Ky,Trang_Thai,Ngay_Dang_Ky)"
                        + " VALUES (?,?,?,'CT_B1','CT_PERIOD','CT_G1','Nhap',?)",
                    id,
                    "Đồ án dữ liệu " + i,
                    "Phân tích dữ liệu khoa FIT",
                    now);
        }
        var context =
                (org.springframework.security.core.context.SecurityContext)
                        session("ct.g0").getAttribute("SPRING_SECURITY_CONTEXT");
        var stats =
                em.getEntityManagerFactory()
                        .unwrap(org.hibernate.SessionFactory.class)
                        .getStatistics();
        boolean enabled = stats.isStatisticsEnabled();
        try {
            stats.setStatisticsEnabled(true);
            org.springframework.security.core.context.SecurityContextHolder.setContext(context);
            em.clear();
            stats.clear();
            var page =
                    topics.all(
                            new com.example.quanlydetai.dto.TopicSearch(
                                    "DO AN DU LIEU", "CT_B1", "CT_PERIOD", "MonHoc", "Nhap"),
                            org.springframework.data.domain.PageRequest.of(1, 7));
            assertThatThrownBy(
                            () ->
                                    topics.all(
                                            com.example.quanlydetai.dto.TopicSearch.empty(),
                                            org.springframework.data.domain.Pageable.unpaged()))
                    .isInstanceOf(com.example.quanlydetai.exception.BusinessException.class);
            assertThat(page.getTotalElements()).isEqualTo(42);
            assertThat(page.getContent())
                    .extracting(com.example.quanlydetai.dto.DeTaiResponse::id)
                    .containsExactly(
                            "CT_Q07", "CT_Q08", "CT_Q09", "CT_Q10", "CT_Q11", "CT_Q12", "CT_Q13");
            assertThat(stats.getEntityLoadCount()).isLessThan(20);
            assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(10);
            System.out.println(
                    "Topic search: total="
                            + page.getTotalElements()
                            + ", page="
                            + page.getNumberOfElements()
                            + ", loaded entities="
                            + stats.getEntityLoadCount()
                            + ", SQL="
                            + stats.getPrepareStatementCount());
            assertThat(
                            topics.all(
                                    new com.example.quanlydetai.dto.TopicSearch(
                                            "du lieu", "CT_B2", "CT_PERIOD", "MonHoc", "Nhap"),
                                    org.springframework.data.domain.PageRequest.of(0, 7)))
                    .isEmpty();
            assertThat(
                            topics.all(
                                    new com.example.quanlydetai.dto.TopicSearch(
                                            "du lieu", "CT_B1", "CT_EMPTY", "MonHoc", "Nhap"),
                                    org.springframework.data.domain.PageRequest.of(0, 7)))
                    .isEmpty();
            assertThat(
                            topics.all(
                                    new com.example.quanlydetai.dto.TopicSearch(
                                            "du lieu", "CT_B1", "CT_PERIOD", "TLCN", "Nhap"),
                                    org.springframework.data.domain.PageRequest.of(0, 7)))
                    .isEmpty();
            assertThat(
                            topics.all(
                                    new com.example.quanlydetai.dto.TopicSearch(
                                            "du lieu", "CT_B1", "CT_PERIOD", "MonHoc", "Huy"),
                                    org.springframework.data.domain.PageRequest.of(0, 7)))
                    .isEmpty();
        } finally {
            stats.setStatisticsEnabled(enabled);
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    void accountSearchJoinsProfilesAndReturnsOnePageWithTotals() throws Exception {
        for (int i = 0; i < 42; i++) {
            String id = "CT_QS" + String.format("%02d", i);
            jdbc.update(
                    "INSERT INTO SV(Ma_SV,Ho_Ten,Email) VALUES (?,?,?)",
                    id,
                    "Nguyễn Đăng Khoa " + i,
                    id + "@example.test");
            account("ct.query" + String.format("%02d", i), "SV", null, id);
        }
        var login = session("ct.admin");
        var stats =
                em.getEntityManagerFactory()
                        .unwrap(org.hibernate.SessionFactory.class)
                        .getStatistics();
        boolean enabled = stats.isStatisticsEnabled();
        try {
            stats.setStatisticsEnabled(true);
            em.clear();
            stats.clear();
            mvc.perform(
                            get("/api/v1/admin/accounts")
                                    .session(login)
                                    .param("q", "nguyen dang")
                                    .param("role", "SV")
                                    .param("page", "1")
                                    .param("size", "7"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Total-Count", "42"))
                    .andExpect(header().string("X-Page", "1"))
                    .andExpect(jsonPath("$.length()").value(7))
                    .andExpect(jsonPath("$[0].profile").value("CT_QS07"))
                    .andExpect(jsonPath("$[0].name").value("Nguyễn Đăng Khoa 7"))
                    .andExpect(jsonPath("$[0].matKhauHash").doesNotExist());
            assertThat(stats.getPrepareStatementCount()).isLessThan(20);
            assertThat(stats.getEntityLoadCount()).isLessThan(30);
            System.out.println(
                    "Account page SQL="
                            + stats.getPrepareStatementCount()
                            + ", loaded entities="
                            + stats.getEntityLoadCount());
        } finally {
            stats.setStatisticsEnabled(enabled);
        }
        mvc.perform(
                        get("/api/v1/admin/accounts")
                                .session(login)
                                .param("q", "nguyen dang")
                                .param("role", "GV"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void keywordSearchEscapesWildcardsAndPagingRejectsInvalidBounds() throws Exception {
        jdbc.update(
                "INSERT INTO THONG_BAO(Ma_TB,Ma_TK,Tieu_De,Noi_Dung,Pham_Vi,Ngay_Dang) VALUES"
                    + " ('CT_QNOTICE','ct.admin','Học bổng 100%_!','Thông tin khoa FIT','Khoa',?)",
                now);
        jdbc.update(
                "INSERT INTO THONG_BAO(Ma_TB,Ma_TK,Tieu_De,Noi_Dung,Pham_Vi,Ngay_Dang) VALUES"
                        + " ('CT_QOTHER','ct.admin','Học bổng 100XYZ','Thông tin khác','Khoa',?)",
                now);
        mvc.perform(
                        get("/api/v1/announcements")
                                .session(session("ct.student"))
                                .param("q", "100%_!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("CT_QNOTICE"));
        mvc.perform(
                        get("/api/v1/announcements")
                                .session(session("ct.student"))
                                .param("q", "' or 1=1 --"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        for (String[] bounds :
                List.of(
                        new String[] {"-1", "25"},
                        new String[] {"0", "0"},
                        new String[] {"0", "101"},
                        new String[] {"2147483647", "100"}))
            mvc.perform(
                            get("/api/v1/admin/accounts")
                                    .session(session("ct.admin"))
                                    .param("page", bounds[0])
                                    .param("size", bounds[1]))
                    .andExpect(status().isBadRequest());
    }

    @Test
    void pagingLinksPreserveTopicFiltersAndIndependentApprovalSections() throws Exception {
        api("ct.g2", "PATCH", "/api/v1/topics/CT_TOPIC/department-review", "{\"accept\":true}")
                .andExpect(status().isNoContent());
        var response =
                mvc.perform(
                                get("/dean/approvals")
                                        .session(session("ct.g0"))
                                        .param("size", "1")
                                        .param("q", "")
                                        .param("department", "CT_B1")
                                        .param("period", "CT_PERIOD")
                                        .param("type", "MonHoc")
                                        .param("state", "Nhap")
                                        .param("registrationsPage", "2")
                                        .param("resultsPage", "3"))
                        .andExpect(status().isOk())
                        .andReturn();
        // Add another matching topic so a real next-page link must be rendered.
        jdbc.update(
                "INSERT INTO"
                    + " DE_TAI(Ma_De_Tai,Ten_De_Tai,Noi_Dung,Ma_Bo_Mon,Ma_Dot,Ma_GV_Dang_Ky,Trang_Thai,Ngay_Dang_Ky)"
                    + " VALUES ('CT_QNEXT','Trang tiếp theo','Nội"
                    + " dung','CT_B1','CT_PERIOD','CT_G1','Nhap',?)",
                now);
        jdbc.update("INSERT INTO GVHD_DETAI(Ma_GV,Ma_De_Tai) VALUES ('CT_G1','CT_QNEXT')");
        api("ct.g2", "PATCH", "/api/v1/topics/CT_QNEXT/department-review", "{\"accept\":true}")
                .andExpect(status().isNoContent());
        response =
                mvc.perform(
                                get("/dean/approvals")
                                        .session(session("ct.g0"))
                                        .param("size", "1")
                                        .param("department", "CT_B1")
                                        .param("period", "CT_PERIOD")
                                        .param("type", "MonHoc")
                                        .param("state", "Nhap")
                                        .param("registrationsPage", "2")
                                        .param("resultsPage", "3"))
                        .andExpect(status().isOk())
                        .andReturn();
        String html =
                response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var matcher =
                java.util.regex.Pattern.compile("href=\"([^\"]+)\">Trang sau</a>").matcher(html);
        assertThat(matcher.find()).isTrue();
        String next = matcher.group(1).replace("&amp;", "&");
        assertThat(next)
                .contains(
                        "page=1",
                        "size=1",
                        "department=CT_B1",
                        "period=CT_PERIOD",
                        "type=MonHoc",
                        "state=",
                        "registrationsPage=2",
                        "resultsPage=3");
        mvc.perform(get(java.net.URI.create(next)).session(session("ct.g0")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CT_TOPIC")));
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
        api(
                        "ct.g3",
                        "PATCH",
                        "/api/v1/topics/CT_TOPIC/department-review",
                        "{\"accept\":true}")
                .andExpect(status().isForbidden());
        api(
                        "ct.g1",
                        "PATCH",
                        "/api/v1/topics/CT_TOPIC/department-review",
                        "{\"accept\":true}")
                .andExpect(status().isForbidden());
        api(
                        "ct.g2",
                        "PATCH",
                        "/api/v1/topics/CT_TOPIC/department-review",
                        "{\"accept\":true}")
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
        api(
                        "ct.g2",
                        "PATCH",
                        "/api/v1/topics/CT_TOPIC/department-review",
                        "{\"accept\":true}")
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
