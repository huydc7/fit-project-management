package com.example.quanlydetai;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.quanlydetai.service.DiemService;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkflowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired EntityManager em;
    @Autowired com.example.quanlydetai.repository.ResultEmailRepository emailRows;
    private static String hash;
    private final Map<String, MockHttpSession> sessions = new HashMap<>();

    @BeforeEach
    void seed() {
        if (hash == null) hash = encoder.encode("Workflow-Test-Password!");
        jdbc.update(
                "INSERT INTO BO_MON(Ma_Bo_Mon,Ten_Bo_Mon) VALUES ('WF_BM','Workflow department')");
        for (int i = 0; i <= 6; i++) {
            String id = "WF_GV" + i;
            jdbc.update(
                    "INSERT INTO GV(Ma_GV,Ho_Ten,Email,Ma_Bo_Mon) VALUES (?,?,?,?)",
                    id,
                    "Teacher " + i,
                    id + "@example.test",
                    "WF_BM");
            account("wf.gv" + i, "GV", id, null);
        }
        jdbc.update("INSERT INTO TRUONG_KHOA(Ma_GV,Dang_Giu_Chuc) VALUES ('WF_GV0',true)");
        for (int i = 1; i <= 10; i++) {
            String id = "WF_SV" + i;
            jdbc.update(
                    "INSERT INTO SV(Ma_SV,Ho_Ten,Email) VALUES (?,?,?)",
                    id,
                    "Student " + i,
                    id + "@example.test");
            account("wf.sv" + i, "SV", null, id);
        }
        account("wf.admin", "Admin", null, null);
        var now = LocalDateTime.now();
        period("WF_SV", now.minusDays(10), now.minusDays(5), now.plusDays(1), now.plusDays(10));
        period("WF_GV", now.minusDays(1), now.plusDays(1), now.plusDays(2), now.plusDays(10));
        if (jdbc.queryForObject(
                        "SELECT COUNT(*) FROM LOAI_DIEM WHERE Ma_Loai_Diem='GVPB'", Integer.class)
                == 0)
            jdbc.update(
                    "INSERT INTO LOAI_DIEM(Ma_Loai_Diem,Ten_Loai_Diem) VALUES"
                            + " ('GVPB','Review'),('HD','Council')");
        jdbc.update(
                "INSERT INTO"
                    + " DE_TAI(Ma_De_Tai,Ten_De_Tai,Ma_Bo_Mon,Noi_Dung,Trang_Thai,Ma_Dot,Ma_GV_Dang_Ky,Ngay_Dang_Ky)"
                    + " VALUES ('WF_DT','Workflow topic','WF_BM','Required"
                    + " content','Nhap','WF_SV','WF_GV1',?)",
                now.minusDays(6));
        jdbc.update("INSERT INTO GVHD_DETAI(Ma_GV,Ma_De_Tai) VALUES ('WF_GV1','WF_DT')");
        jdbc.update("UPDATE DE_TAI SET Trang_Thai='DaCongBo' WHERE Ma_De_Tai='WF_DT'");
        // Fixture advances from pre-publication into student registration, inside the test DB only.
        jdbc.update("UPDATE DOT_DANG_KY SET Begin_SV=? WHERE Ma_Dot='WF_SV'", now.minusDays(4));
    }

    @Test
    void apiUsesJsonForAuthenticationRolesAndCsrfFailures() throws Exception {
        mvc.perform(get("/api/v1/groups"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/v1/groups").session(session("wf.gv1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mvc.perform(
                        post("/api/v1/groups")
                                .session(session("wf.sv1"))
                                .contentType("application/json")
                                .content("{\"period\":\"WF_SV\",\"name\":\"Nhóm API\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM NHOM", Integer.class)).isZero();
    }

    @Test
    void apiCreatesGroupSharedWithHtmlAndRejectsAnotherStudentsChanges() throws Exception {
        mvc.perform(
                        post("/api/v1/groups")
                                .session(session("wf.sv1"))
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"period\":\"WF_SV\",\"name\":\"Nhóm API\"}"))
                .andExpect(status().isCreated());
        String id =
                jdbc.queryForObject(
                        "SELECT Ma_Nhom FROM NHOM WHERE Ten_Nhom='Nhóm API'", String.class);
        mvc.perform(get("/api/v1/groups").session(session("wf.sv1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].members[0].id").value("WF_SV1"));
        mvc.perform(get("/api/v1/groups").session(session("wf.sv2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(
                        post("/api/v1/groups/" + id + "/invitations")
                                .session(session("wf.sv2"))
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"student\":\"WF_SV3\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mvc.perform(get("/student/groups").session(session("wf.sv1")))
                .andExpect(status().isOk())
                .andExpect(view().name("sv/groups"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nhóm API")));
    }

    @Test
    void apiValidatesJsonAndDoesNotEchoRejectedPassword() throws Exception {
        mvc.perform(
                        post("/api/v1/admin/accounts")
                                .session(session("wf.admin"))
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        "{\"username\":\"new.account\",\"password\":\"secret\",\"role\":\"SV\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString("secret"))));
        mvc.perform(
                        post("/api/v1/groups")
                                .session(session("wf.sv1"))
                                .with(csrf())
                                .contentType("application/json")
                                .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mvc.perform(
                        post("/api/v1/groups")
                                .session(session("wf.sv1"))
                                .with(csrf())
                                .contentType("text/plain")
                                .content("invalid"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void apiPreservesBusinessRulesAndRequiresExplicitReviewDecision() throws Exception {
        mvc.perform(
                        post("/api/v1/groups")
                                .session(session("wf.sv1"))
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"period\":\"WF_GV\",\"name\":\"Too early\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
        mvc.perform(
                        patch("/api/v1/topics/WF_DT/review")
                                .session(session("wf.gv0"))
                                .with(csrf())
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.accept").exists());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Trang_Thai FROM DE_TAI WHERE Ma_De_Tai='WF_DT'",
                                String.class))
                .isEqualTo("DaCongBo");
    }

    @Test
    void apiExpiresLockedSessionsWithJsonAndProtectsReports() throws Exception {
        var session = session("wf.sv1");
        jdbc.update("UPDATE TAI_KHOAN SET Trang_Thai='Khoa' WHERE Ten_Dang_Nhap='wf.sv1'");
        em.clear();
        mvc.perform(get("/api/v1/groups").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/v1/reports/mine").session(session("wf.admin")))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiProvidesCsrfTokenAndReturnsOnlyAccountDtos() throws Exception {
        mvc.perform(get("/api/v1/session/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/v1/admin/accounts").session(session("wf.admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").exists())
                .andExpect(jsonPath("$[0].matKhauHash").doesNotExist())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(hash))));
    }

    @Test
    void apiUsesRealSessionAndCsrfTokensAcrossLoginAndReturnsJsonRoutingErrors() throws Exception {
        var anonymous = mvc.perform(get("/api/v1/session/csrf")).andReturn();
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        String token =
                mapper.readTree(anonymous.getResponse().getContentAsString()).get("token").asString();
        var loggedIn =
                mvc.perform(
                                post("/login")
                                        .session(
                                                (MockHttpSession)
                                                        anonymous.getRequest().getSession(false))
                                        .param("username", "wf.sv1")
                                        .param("password", "Workflow-Test-Password!")
                                        .param("_csrf", token))
                        .andExpect(authenticated())
                        .andReturn();
        var session = (MockHttpSession) loggedIn.getRequest().getSession(false);
        var csrf = mvc.perform(get("/api/v1/session/csrf").session(session)).andReturn();
        String currentToken =
                mapper.readTree(csrf.getResponse().getContentAsString()).get("token").asString();
        mvc.perform(
                        post("/api/v1/groups")
                                .session(session)
                                .header("X-CSRF-TOKEN", currentToken)
                                .contentType("application/json")
                                .content("{\"period\":\"WF_SV\",\"name\":\"Session API\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/khong-ton-tai").session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(delete("/api/v1/groups").session(session).header("X-CSRF-TOKEN", currentToken))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    private void account(String name, String role, String gv, String sv) {
        jdbc.update(
                "INSERT INTO"
                    + " TAI_KHOAN(Ma_TK,Ten_Dang_Nhap,Mat_Khau_Hash,Vai_Tro,Trang_Thai,Ma_GV,Ma_SV,Ngay_Tao)"
                    + " VALUES (?,?,?,?,?,?,?,?)",
                name,
                name,
                hash,
                role,
                "HoatDong",
                gv,
                sv,
                LocalDateTime.now());
    }

    private void period(
            String id, LocalDateTime a, LocalDateTime b, LocalDateTime c, LocalDateTime d) {
        jdbc.update(
                "INSERT INTO"
                    + " DOT_DANG_KY(Ma_Dot,Ma_Truong_Khoa,Ten_Dot_Dang_Ky,Loai,Begin_GV,End_GV,Begin_SV,End_SV)"
                    + " VALUES (?,'WF_GV0',?,'MonHoc',?,?,?,?)",
                id,
                id,
                a,
                b,
                c,
                d);
    }

    private MockHttpSession session(String username) throws Exception {
        if (!sessions.containsKey(username))
            sessions.put(
                    username,
                    (MockHttpSession)
                            mvc.perform(
                                            formLogin()
                                                    .user(username)
                                                    .password("Workflow-Test-Password!"))
                                    .andExpect(authenticated())
                                    .andReturn()
                                    .getRequest()
                                    .getSession(false));
        return sessions.get(username);
    }

    private ResultActions action(String user, String path, String... params) throws Exception {
        var request = post(path).session(session(user)).with(csrf());
        for (int i = 0; i < params.length; i += 2) request.param(params[i], params[i + 1]);
        return mvc.perform(request);
    }

    private void ok(String user, String path, String... params) throws Exception {
        action(user, path, params)
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("thanhCong"));
    }

    private String group(String user) throws Exception {
        ok(user, "/student/groups", "period", "WF_SV", "name", "Group " + user);
        return jdbc.queryForObject(
                "SELECT m.Ma_Nhom FROM THANH_VIEN_NHOM m JOIN TAI_KHOAN t ON t.Ma_SV=m.Ma_SV WHERE"
                        + " t.Ten_Dang_Nhap=?",
                String.class,
                user);
    }


    @Test
    void registrationReviewRequiresDeanInHtmlAndApiEvenForTopicSupervisor() throws Exception {
        String id = pendingRegistration();
        mvc.perform(get("/dean/registrations").session(session("wf.gv0")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("registration-" + id)));
        mvc.perform(get("/api/v1/registrations/pending").session(session("wf.gv0")))
                .andExpect(jsonPath("$[0].id").value(id));
        for (String user : List.of("wf.gv1", "wf.gv2", "wf.sv1")) {
            mvc.perform(get("/dean/registrations").session(session(user)))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/registrations/pending").session(session(user)))
                    .andExpect(status().isForbidden());
            action(user, "/dean/registrations/" + id + "/review", "accept", "true", "reason", "")
                    .andExpect(status().isForbidden());
            mvc.perform(patch("/api/v1/registrations/" + id + "/review").session(session(user))
                            .with(csrf()).contentType("application/json").content("{\"accept\":true}"))
                    .andExpect(status().isForbidden());
        }
        assertThat(jdbc.queryForObject("SELECT Trang_Thai FROM DANG_KY_DE_TAI WHERE Ma_DK=?", String.class, id))
                .isEqualTo("ChoDuyet");
        mvc.perform(patch("/api/v1/registrations/" + id + "/review").session(session("wf.gv0"))
                        .with(csrf()).contentType("application/json").content("{\"accept\":true}"))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT Ma_GV_Duyet FROM DANG_KY_DE_TAI WHERE Ma_DK=?", String.class, id))
                .isEqualTo("WF_GV0");
        action("wf.gv0", "/dean/registrations/" + id + "/review", "accept", "false", "reason", "Too late")
                .andExpect(flash().attributeExists("loi"));
    }

    private String pendingRegistration() throws Exception {
        String group = group("wf.sv1");
        join(group, "wf.sv1", 7, 8);
        ok("wf.sv1", "/student/registrations", "group", group, "topic", "WF_DT");
        return jdbc.queryForObject("SELECT Ma_DK FROM DANG_KY_DE_TAI WHERE Active_Group=?", String.class, group);
    }

    @Test
    void rejectionRequiresReasonAndShowsErrorBesideTheCorrectRegistration() throws Exception {
        String id = pendingRegistration();
        for (String reason : List.of("", "   ", "x".repeat(1001))) {
            var result = action("wf.gv0", "/dean/registrations/" + id + "/review",
                    "accept", "false", "reason", reason, "registrationsPage", "0", "size", "1")
                    .andExpect(flash().attributeExists("loi"))
                    .andExpect(flash().attribute("registrationErrorId", id))
                    .andExpect(redirectedUrl("/dean/registrations?registrationsPage=0&size=1#registration-" + id))
                    .andReturn();
            mvc.perform(get("/dean/registrations").param("size", "1").session(session("wf.gv0"))
                            .flashAttrs(result.getFlashMap()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.matchesRegex(
                            "(?s).*<p[^>]*id=\"reason-error-" + id + "\"[^>]*>Từ chối phải ghi lý do \\(tối đa 1.000 ký tự\\)\\.</p>.*")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-invalid=\"true\"")));
        }
        mvc.perform(patch("/api/v1/registrations/" + id + "/review").session(session("wf.gv0"))
                        .with(csrf()).contentType("application/json").content("{\"accept\":false,\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT Trang_Thai FROM DANG_KY_DE_TAI WHERE Ma_DK=?", String.class, id)).isEqualTo("ChoDuyet");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM THONG_BAO", Integer.class)).isZero();
    }

    @Test
    void rejectionNoticesArePrivateAcrossHtmlApiSearchCountsAndHome() throws Exception {
        String id = pendingRegistration();
        String reason = "PRIVATE_REJECTION_713: Cần bổ sung phạm vi thực hiện <script>alert(1)</script>";
        ok("wf.gv0", "/dean/registrations/" + id + "/review", "accept", "false", "reason", reason);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM THONG_BAO", Integer.class)).isZero();
        for (String user : List.of("wf.sv1")) {
            mvc.perform(get("/api/v1/announcements").param("q", "PRIVATE_REJECTION_713").param("size", "1").session(session(user)))
                    .andExpect(header().string("X-Total-Count", "1"))
                    .andExpect(jsonPath("$[0].content").value(org.hamcrest.Matchers.containsString(reason)))
                    .andExpect(jsonPath("$[0].manageable").value(false));
            mvc.perform(get("/announcements").session(session(user)))
                        .andExpect(content().string(org.hamcrest.Matchers.containsString("PRIVATE_REJECTION_713")))
                        .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script>alert(1)</script>"))));
            mvc.perform(get("/").session(session(user)))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Đăng ký đề tài của nhóm bị từ chối")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Riêng cho nhóm trưởng")));
        }
        for (String user : List.of("wf.sv2", "wf.sv7", "wf.sv8", "wf.gv1", "wf.gv0", "wf.admin")) {
            mvc.perform(get("/api/v1/announcements").param("q", "PRIVATE_REJECTION_713").session(session(user)))
                    .andExpect(header().string("X-Total-Count", "0"))
                    .andExpect(jsonPath("$.length()").value(0));
            for (String path : List.of("/announcements", "/"))
                mvc.perform(get(path).session(session(user)))
                        .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("PRIVATE_REJECTION_713"))))
                        .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Đăng ký đề tài của nhóm bị từ chối"))));
        }
        // A later membership change must not reveal the earlier private decision to a newcomer.
        String group = jdbc.queryForObject("SELECT Ma_Nhom FROM DANG_KY_DE_TAI WHERE Ma_DK=?", String.class, id);
        ok("wf.sv7", "/student/groups/" + group + "/members/remove", "student", "WF_SV7");
        join(group, "wf.sv1", 2);
        mvc.perform(get("/api/v1/announcements").session(session("wf.sv2")))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/announcements").session(session("wf.sv7")))
                .andExpect(jsonPath("$.length()").value(0));
        String notice = "registration:" + id;
        mvc.perform(delete("/api/v1/announcements/" + notice).session(session("wf.admin")).with(csrf()))
                .andExpect(status().isForbidden());
        action("wf.gv0", "/dean/registrations/" + id + "/review", "accept", "false", "reason", reason)
                .andExpect(flash().attributeExists("loi"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM THONG_BAO", Integer.class)).isZero();
    }

    @Test
    void acceptingWithoutReasonDoesNotCreateRejectionNotifications() throws Exception {
        registered();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM THONG_BAO", Integer.class)).isZero();
    }

    @Test
    void mixedAnnouncementFeedPaginatesBothSourcesWithoutDuplicatesOrPrivateCountsForOthers() throws Exception {
        String registration = pendingRegistration();
        ok("wf.gv0", "/dean/registrations/" + registration + "/review", "accept", "false", "reason", "MIXED_FEED_TEST");
        LocalDateTime posted = jdbc.queryForObject("SELECT Ngay_Duyet FROM DANG_KY_DE_TAI WHERE Ma_DK=?",
                LocalDateTime.class, registration);
        for (int i = 1; i <= 3; i++) {
            jdbc.update("INSERT INTO THONG_BAO(Ma_TB,Tieu_De,Noi_Dung,Ngay_Dang,Pham_Vi,Ma_TK) VALUES (?,?,?,?,?,?)",
                    "WF_NOTICE" + i, "MIXED_FEED_TEST " + i, "Public notice", posted.plusMinutes(i - 2), "Khoa", "wf.gv0");
            // MySQL's insert trigger stamps the current time; set the intended test clock afterward.
            jdbc.update("UPDATE THONG_BAO SET Ngay_Dang=? WHERE Ma_TB=?", posted.plusMinutes(i - 2), "WF_NOTICE" + i);
        }
        var ids = new ArrayList<String>();
        for (int page = 0; page < 4; page++) {
            var result = mvc.perform(get("/api/v1/announcements").param("q", "MIXED_FEED_TEST")
                            .param("size", "1").param("page", String.valueOf(page)).session(session("wf.sv1")))
                    .andExpect(header().string("X-Total-Count", "4"))
                    .andExpect(jsonPath("$.length()").value(1)).andReturn();
            var rows = new tools.jackson.databind.json.JsonMapper().readTree(result.getResponse().getContentAsString());
            ids.add(rows.get(0).get("id").asString());
        }
        assertThat(ids.getFirst()).isEqualTo("WF_NOTICE3");
        assertThat(ids.getLast()).isEqualTo("WF_NOTICE1");
        // Equal-time IDs follow the database collation, which differs between H2 and MySQL.
        assertThat(ids.subList(1, 3)).containsExactlyInAnyOrder("WF_NOTICE2", "registration:" + registration);
        mvc.perform(get("/api/v1/announcements").param("q", "MIXED_FEED_TEST").session(session("wf.sv2")))
                .andExpect(header().string("X-Total-Count", "3"))
                .andExpect(jsonPath("$[?(@.scope == 'Riêng cho nhóm trưởng')]").isEmpty());
        mvc.perform(get("/api/v1/announcements").param("q", "MIXED_FEED_TEST").param("page", "4")
                        .param("size", "1").session(session("wf.sv1")))
                .andExpect(header().string("X-Total-Count", "4"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    private String registered() throws Exception {
        String group = group("wf.sv1");
        join(group, "wf.sv1", 7, 8);
        ok("wf.sv1", "/student/registrations", "group", group, "topic", "WF_DT");
        String id =
                jdbc.queryForObject(
                        "SELECT Ma_DK FROM DANG_KY_DE_TAI WHERE Ma_Nhom=?", String.class, group);
        ok("wf.gv0", "/dean/registrations/" + id + "/review", "accept", "true", "reason", "");
        return group;
    }

    @Test
    void topicDetailsShowGroupAndCouncilMembersWithinTheAuthorizedTopicScope() throws Exception {
        String group = registered();
        ok("wf.gv0", "/dean/topics/WF_DT/reviewer", "teacher", "WF_GV2");
        ok("wf.gv0", "/dean/councils", "topic", "WF_DT", "period", "WF_SV",
                "chair", "WF_GV3", "secretary", "WF_GV4", "members", "WF_GV5");
        var response = mvc.perform(get("/dean/topics").session(session("wf.gv0")))
                .andExpect(status().isOk()).andReturn();
        @SuppressWarnings("unchecked")
        var rows = (List<com.example.quanlydetai.dto.DeTaiResponse>) response.getModelAndView().getModel().get("topics");
        var topic = rows.stream().filter(t -> "WF_DT".equals(t.id())).findFirst().orElseThrow();
        assertThat(topic.group()).isEqualTo(group);
        assertThat(topic.groupName()).isEqualTo(jdbc.queryForObject("SELECT Ten_Nhom FROM NHOM WHERE Ma_Nhom=?", String.class, group));
        assertThat(topic.proposerName()).isEqualTo("Teacher 1");
        assertThat(topic.supervisorIds()).containsExactly("WF_GV1");
        assertThat(topic.groupMembers()).extracting(m -> m.id()).containsExactly("WF_SV1", "WF_SV7", "WF_SV8");
        assertThat(topic.groupMembers()).extracting(m -> m.roleLabel()).containsExactly("Nhóm trưởng", "Thành viên", "Thành viên");
        assertThat(topic.councilMembers()).extracting(m -> m.id()).containsExactly("WF_GV3", "WF_GV4", "WF_GV5");
        assertThat(topic.councilMembers()).extracting(m -> m.roleLabel()).containsExactly("Chủ tịch hội đồng", "Thư ký hội đồng", "Ủy viên hội đồng");
        String html = response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(html).contains("Student 7", "Student 8", "Teacher 3", "Chủ tịch hội đồng", "Thư ký hội đồng");
        mvc.perform(get("/lecturer/topic-management").session(session("wf.gv1")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Student 7")));
        mvc.perform(get("/lecturer/topic-management").session(session("wf.gv6")))
                .andExpect(model().attribute("topics", org.hamcrest.Matchers.empty()))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Student 7"))));
    }

    private void join(String group, String leader, int... students) throws Exception {
        for (int student : students) {
            ok(leader, "/student/groups/" + group + "/invitations", "student", "WF_SV" + student);
            String id =
                    jdbc.queryForObject(
                            "SELECT Ma_Loi_Moi FROM LOI_MOI_NHOM WHERE Ma_Nhom=? AND Ma_SV=?",
                            String.class,
                            group,
                            "WF_SV" + student);
            ok("wf.sv" + student, "/student/invitations/" + id, "accept", "true");
        }
    }

    private String council() throws Exception {
        // Existing general councils remain supported by the API; topic-specific UI is tested below.
        mvc.perform(post("/api/v1/councils").session(session("wf.gv0")).with(csrf())
                        .contentType("application/json").content(councilBody("WF_SV", "WF_GV3", "WF_GV4", "WF_GV5")))
                .andExpect(status().isCreated());
        String id =
                jdbc.queryForObject(
                        "SELECT Ma_HD FROM HOI_DONG WHERE Ma_Dot='WF_SV'", String.class);
        mvc.perform(patch("/api/v1/councils/" + id + "/finalize").session(session("wf.gv0")).with(csrf()))
                .andExpect(status().isNoContent());
        return id;
    }

    private String councilBody(String period, String chair, String secretary, String member) {
        return "{\"period\":\"" + period + "\",\"chair\":\"" + chair + "\",\"secretary\":\""
                + secretary + "\",\"members\":[\"" + member + "\"]}";
    }

    private String topicCouncil() throws Exception {
        ok("wf.gv0", "/dean/councils", "topic", "WF_DT", "period", "WF_SV",
                "chair", "WF_GV3", "secretary", "WF_GV4", "members", "WF_GV5");
        return jdbc.queryForObject("SELECT Ma_HD FROM HOI_DONG_DE_TAI WHERE Ma_De_Tai='WF_DT'", String.class);
    }

    @Test
    void councilFormRequiresTopicAndExcludesOnlyItsSupervisorsAndReviewerFromEveryRole() throws Exception {
        registered();
        ok("wf.gv0", "/dean/topics/WF_DT/reviewer", "teacher", "WF_GV2");
        jdbc.update("INSERT INTO DE_TAI(Ma_De_Tai,Ten_De_Tai,Noi_Dung,Ma_Bo_Mon,Ma_Dot,Ma_GV_Dang_Ky,Trang_Thai,Ngay_Dang_Ky)"
                + " VALUES ('WF_OTHER','Other supervised topic','Other topic','WF_BM','WF_GV','WF_GV3','Nhap',?)", LocalDateTime.now());
        jdbc.update("INSERT INTO GVHD_DETAI(Ma_GV,Ma_De_Tai) VALUES ('WF_GV3','WF_OTHER')");
        jdbc.update("UPDATE DE_TAI SET Trang_Thai='DaCongBo' WHERE Ma_De_Tai='WF_OTHER'");
        jdbc.update("UPDATE TAI_KHOAN SET Trang_Thai='Khoa' WHERE Ma_TK='wf.gv6'");
        em.clear();
        mvc.perform(get("/dean/councils").session(session("wf.gv0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đề tài cần lập hội đồng")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"chair\""))));
        mvc.perform(get("/dean/councils").param("topic", "WF_DT").session(session("wf.gv0")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"chair\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"secretary\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"WF_GV3\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("value=\"WF_GV1\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("value=\"WF_GV2\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("value=\"WF_GV6\""))));
        mvc.perform(get("/api/v1/councils/topics/WF_DT/candidates").session(session("wf.gv0")))
                .andExpect(jsonPath("$[?(@.id == 'WF_GV1')]").isEmpty())
                .andExpect(jsonPath("$[?(@.id == 'WF_GV2')]").isEmpty())
                .andExpect(jsonPath("$[?(@.id == 'WF_GV6')]").isEmpty())
                .andExpect(jsonPath("$[?(@.id == 'WF_GV3')]").isNotEmpty());
        mvc.perform(get("/api/v1/councils/topics/WF_DT/candidates").session(session("wf.gv1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void forgedCouncilMembersAreRejectedForChairSecretaryAndOrdinaryMember() throws Exception {
        registered();
        ok("wf.gv0", "/dean/topics/WF_DT/reviewer", "teacher", "WF_GV2");
        mvc.perform(post("/api/v1/councils/topics/WF_DT").session(session("wf.gv0")).with(csrf())
                        .contentType("application/json").content(councilBody("WF_GV", "WF_GV3", "WF_GV4", "WF_GV5")))
                .andExpect(status().isBadRequest());
        for (String forbidden : List.of("WF_GV1", "WF_GV2")) {
            for (int position = 0; position < 3; position++) {
                String[] members = {"WF_GV3", "WF_GV4", "WF_GV5"};
                members[position] = forbidden;
                action("wf.gv0", "/dean/councils", "topic", "WF_DT", "period", "WF_SV",
                        "chair", members[0], "secretary", members[1], "members", members[2])
                        .andExpect(status().isOk()).andExpect(model().attributeExists("loi"));
                mvc.perform(post("/api/v1/councils/topics/WF_DT").session(session("wf.gv0")).with(csrf())
                                .contentType("application/json").content(councilBody("WF_SV", members[0], members[1], members[2])))
                        .andExpect(status().isBadRequest());
            }
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM HOI_DONG WHERE Ma_Dot='WF_SV'", Integer.class)).isZero();
    }

    @Test
    void topicCouncilIsCreatedFinalizedAndAssignedTogetherWithoutSchemaExtension() throws Exception {
        registered();
        String id = topicCouncil();
        assertThat(jdbc.queryForObject("SELECT Trang_Thai FROM HOI_DONG WHERE Ma_HD=?", String.class, id)).isEqualTo("DaChot");
        assertThat(jdbc.queryForObject("SELECT Ma_HD FROM HOI_DONG_DE_TAI WHERE Ma_De_Tai='WF_DT'", String.class)).isEqualTo(id);
        mvc.perform(get("/dean/councils/" + id + "/edit").session(session("wf.gv0")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changedReviewerPreventsFinalizationWithoutLockingUnusableCouncil() throws Exception {
        registered();
        mvc.perform(post("/api/v1/councils").session(session("wf.gv0")).with(csrf())
                        .contentType("application/json").content(councilBody("WF_SV", "WF_GV3", "WF_GV4", "WF_GV5")))
                .andExpect(status().isCreated());
        String id = jdbc.queryForObject("SELECT Ma_HD FROM HOI_DONG WHERE Ma_Dot='WF_SV'", String.class);
        ok("wf.gv0", "/dean/topics/WF_DT/reviewer", "teacher", "WF_GV3");
        action("wf.gv0", "/dean/councils/" + id + "/finalize", "topic", "WF_DT")
                .andExpect(flash().attribute("loi", org.hamcrest.Matchers.containsString("GVPB")));
        assertThat(jdbc.queryForObject("SELECT Trang_Thai FROM HOI_DONG WHERE Ma_HD=?", String.class, id)).isEqualTo("DangLap");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM HOI_DONG_DE_TAI WHERE Ma_De_Tai='WF_DT'", Integer.class)).isZero();
        ok("wf.gv0", "/dean/councils/" + id + "/edit", "period", "WF_SV",
                "topic", "WF_DT", "chair", "WF_GV6", "secretary", "WF_GV4", "members", "WF_GV5");
        ok("wf.gv0", "/dean/councils/" + id + "/finalize", "topic", "WF_DT");
        assertThat(jdbc.queryForObject("SELECT Ma_HD FROM HOI_DONG_DE_TAI WHERE Ma_De_Tai='WF_DT'", String.class)).isEqualTo(id);
    }

    @Test
    void incompatibleFinalizedCouncilsAreAbsentFromTopicAssignmentOptions() throws Exception {
        registered();
        ok("wf.gv0", "/dean/topics/WF_DT/reviewer", "teacher", "WF_GV2");
        var ids = new ArrayList<String>();
        for (String chair : List.of("WF_GV1", "WF_GV2", "WF_GV3")) {
            mvc.perform(post("/api/v1/councils").session(session("wf.gv0")).with(csrf())
                            .contentType("application/json").content(councilBody("WF_SV", chair, "WF_GV4", "WF_GV5")))
                    .andExpect(status().isCreated());
            String id = jdbc.queryForObject("SELECT Ma_HD FROM THANHVIEN_HOIDONG WHERE Ma_GV=? AND Vai_Tro='ChuTich'", String.class, chair);
            ids.add(id);
            mvc.perform(patch("/api/v1/councils/" + id + "/finalize").session(session("wf.gv0")).with(csrf()))
                .andExpect(status().isNoContent());
        }
        var result = mvc.perform(get("/dean/topics").session(session("wf.gv0")))
                .andExpect(status().isOk()).andReturn();
        String html = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(html).contains("value=\"" + ids.get(2) + "\"")
                .doesNotContain("value=\"" + ids.get(0) + "\"", "value=\"" + ids.get(1) + "\"");
    }

    @Test
    void oldDraftRequiresTopicForEditingAndAgainForFinalAssignment() throws Exception {
        registered();
        mvc.perform(post("/api/v1/councils").session(session("wf.gv0")).with(csrf())
                        .contentType("application/json").content(councilBody("WF_SV", "WF_GV3", "WF_GV4", "WF_GV5")))
                .andExpect(status().isCreated());
        String id = jdbc.queryForObject("SELECT Ma_HD FROM HOI_DONG WHERE Ma_Dot='WF_SV'", String.class);
        mvc.perform(get("/dean/councils/" + id + "/edit").session(session("wf.gv0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("lọc giảng viên trước khi sửa hội đồng nháp")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"chair\""))));
        mvc.perform(get("/dean/councils/" + id + "/edit").param("topic", "WF_DT").session(session("wf.gv0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"chair\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("value=\"WF_GV1\""))));
        ok("wf.gv0", "/dean/councils/" + id + "/edit", "topic", "WF_DT", "period", "WF_SV",
                "chair", "WF_GV3", "secretary", "WF_GV4", "members", "WF_GV5");
        action("wf.gv0", "/dean/councils/" + id + "/finalize").andExpect(flash().attributeExists("loi"));
        assertThat(jdbc.queryForObject("SELECT Trang_Thai FROM HOI_DONG WHERE Ma_HD=?", String.class, id)).isEqualTo("DangLap");
        ok("wf.gv0", "/dean/councils/" + id + "/finalize", "topic", "WF_DT");
        assertThat(jdbc.queryForObject("SELECT Ma_HD FROM HOI_DONG_DE_TAI WHERE Ma_De_Tai='WF_DT'", String.class)).isEqualTo(id);
    }

    private ResultActions upload(String user, String group, String token) throws Exception {
        return mvc.perform(
                multipart("/student/reports")
                        .file(
                                new MockMultipartFile(
                                        "file",
                                        "report.pdf",
                                        "application/pdf",
                                        "%PDF-1.7\nTest report".getBytes()))
                        .param("group", group)
                        .param("token", token)
                        .session(session(user))
                        .with(csrf()));
    }

    @Test
    void operationGroupChoicesAreNotCutOffByHistoryPagination() throws Exception {
        String target = group("wf.sv1");
        var now = LocalDateTime.now();
        for (int i = 0; i < 26; i++) {
            String period = "WP_OPT" + i, group = "AA_OPT" + String.format("%02d", i);
            period(period, now.minusDays(10), now.minusDays(5), now.minusDays(4), now.plusDays(5));
            jdbc.update(
                    "INSERT INTO NHOM(Ma_Nhom,Ten_Nhom,Ma_Dot) VALUES (?,?,?)",
                    group,
                    "Other group " + i,
                    period);
            jdbc.update(
                    "INSERT INTO THANH_VIEN_NHOM(Ma_Nhom,Ma_SV,Vai_Tro) VALUES"
                            + " (?,'WF_SV1','Truong_Nhom')",
                    group);
        }
        mvc.perform(get("/student/groups").session(session("wf.sv1")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("groups", org.hamcrest.Matchers.hasSize(25)));
        mvc.perform(get("/student/topics").session(session("wf.sv1")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "value=\"" + target + "\"")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "value=\"AA_OPT00\""))));
        join(target, "wf.sv1", 7, 8);
        ok("wf.sv1", "/student/registrations", "group", target, "topic", "WF_DT");
        String registration =
                jdbc.queryForObject(
                        "SELECT Ma_DK FROM DANG_KY_DE_TAI WHERE Active_Group=?",
                        String.class,
                        target);
        ok(
                "wf.gv0",
                "/dean/registrations/" + registration + "/review",
                "accept",
                "true",
                "reason",
                "");
        mvc.perform(get("/student/reports").session(session("wf.sv1")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "value=\"" + target + "\"")));
    }

    @Test
    void reportPagingDoesNotMaterializeTheEntireHistory() throws Exception {
        String group = registered();
        for (int i = 1; i <= 40; i++) {
            String token = String.format("%024d", i);
            jdbc.update(
                    "INSERT INTO"
                        + " BAO_CAO(Ma_BC,Ma_Nhom,Ma_SV_Nguoi_Nop,File_Dinh_Kem,Lan_Nop,Ngay_Nop)"
                        + " VALUES (?,?,'WF_SV1',?,?,?)",
                    token,
                    group,
                    "fixtures/" + token + ".pdf",
                    i,
                    LocalDateTime.now());
        }
        var login = session("wf.gv1");
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
                            get("/api/v1/reports/assigned")
                                    .session(login)
                                    .param("size", "5")
                                    .param("page", "1"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Total-Count", "40"))
                    .andExpect(jsonPath("$.length()").value(5));
            assertThat(stats.getEntityLoadCount()).isLessThan(20);
            assertThat(stats.getPrepareStatementCount()).isLessThan(20);
            System.out.println(
                    "Report page SQL="
                            + stats.getPrepareStatementCount()
                            + ", loaded entities="
                            + stats.getEntityLoadCount());
        } finally {
            stats.setStatisticsEnabled(enabled);
        }
    }

    @Test
    void reportPagesRespectAssignmentsAndGradingUsesOnlyLatestVersion() throws Exception {
        String group = registered();
        String council = council();
        ok("wf.gv0", "/dean/topics/WF_DT/reviewer", "teacher", "WF_GV2");
        ok("wf.gv0", "/dean/topics/WF_DT/councils", "council", council);
        upload("wf.sv1", group, "111111111111111111111111")
                .andExpect(flash().attributeExists("thanhCong"));
        upload("wf.sv1", group, "222222222222222222222222")
                .andExpect(flash().attributeExists("thanhCong"));
        for (String user : List.of("wf.gv0", "wf.gv1", "wf.gv2", "wf.gv3"))
            mvc.perform(get("/api/v1/reports/assigned").session(session(user)).param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Total-Count", "2"))
                    .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(
                        get("/api/v1/reports/assigned")
                                .session(session("wf.gv6"))
                                .param("teacher", "WF_GV1")
                                .param("dean", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"))
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/v1/reports/mine").session(session("wf.sv2")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"));
        mvc.perform(
                        get("/api/v1/reports/mine")
                                .session(session("wf.sv7"))
                                .param("page", "1")
                                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/grades/ballots").session(session("wf.gv2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reports.length()").value(1))
                .andExpect(jsonPath("$[0].reports[0].version").value(2))
                .andExpect(jsonPath("$[0].required").value(4));
        mvc.perform(get("/lecturer/reports").session(session("wf.gv1")).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trang sau")));
    }

    @Test
    void reviewerCannotStartGradingUntilCouncilIsAssigned() throws Exception {
        verifyIncompleteAssignmentCannotLockTopic(true);
    }

    @Test
    void councilCannotStartGradingUntilReviewerIsAssigned() throws Exception {
        verifyIncompleteAssignmentCannotLockTopic(false);
    }

    private void verifyIncompleteAssignmentCannotLockTopic(boolean reviewerFirst) throws Exception {
        String group = registered();
        String council = council();
        String teacher = reviewerFirst ? "wf.gv2" : "wf.gv3";
        String type = reviewerFirst ? "GVPB" : "HD";
        if (reviewerFirst)
            ok("wf.gv0", "/dean/topics/WF_DT/reviewer", "teacher", "WF_GV2");
        else ok("wf.gv0", "/dean/topics/WF_DT/councils", "council", council);
        upload("wf.sv1", group, "eeeeeeeeeeeeeeeeeeeeeeee")
                .andExpect(flash().attributeExists("thanhCong"));

        String message = "Cần phân công đủ GVPB và hội đồng đã chốt trước khi chấm điểm.";
        action(teacher, "/lecturer/grading", "topic", "WF_DT", "type", type,
                        "value", "8", "comment", "Premature grading")
                .andExpect(status().isOk())
                .andExpect(model().attribute("loi", message));
        mvc.perform(put("/api/v1/grades/ballots").session(session(teacher)).with(csrf())
                        .contentType("application/json")
                        .content("{\"topic\":\"WF_DT\",\"type\":\"" + type
                                + "\",\"value\":8,\"comment\":\"Premature grading\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(message));
        mvc.perform(get("/api/v1/grades/ballots").session(session(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assignmentsComplete").value(false));
        mvc.perform(get("/lecturer/grading").session(session(teacher)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(message)))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("name=\"value\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("name=\"comment\""))));
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM DIEM_THANH_PHAN WHERE Ma_De_Tai='WF_DT'", Integer.class)).isZero();
        // Rejected grading must not lock report revisions or prevent completing assignments.
        upload("wf.sv1", group, "ffffffffffffffffffffffff")
                .andExpect(flash().attributeExists("thanhCong"));
        if (reviewerFirst)
            ok("wf.gv0", "/dean/topics/WF_DT/councils", "council", council);
        else ok("wf.gv0", "/dean/topics/WF_DT/reviewer", "teacher", "WF_GV2");
        mvc.perform(get("/api/v1/grades/ballots").session(session(teacher)))
                .andExpect(jsonPath("$[0].assignmentsComplete").value(true))
                .andExpect(jsonPath("$[0].locked").value(false));
        mvc.perform(get("/lecturer/grading").session(session(teacher)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"value\"")));
        // The formerly blocked topic can now complete the entire grading/publication cycle.
        for (int i = 2; i <= 5; i++)
            ok("wf.gv" + i, "/lecturer/grading", "topic", "WF_DT", "type", i == 2 ? "GVPB" : "HD",
                    "value", i == 2 ? "9" : "6", "comment", "Completed assignment");
        action("wf.gv0", "/dean/topics/WF_DT/reviewer", "teacher", "WF_GV6")
                .andExpect(flash().attribute("loi", "Phân công đã khóa khi bắt đầu chấm điểm."));
        ok("wf.gv3", "/lecturer/topics/WF_DT/aggregate", "comment", "Complete grading");
        ok("wf.gv0", "/dean/topics/WF_DT/publish");
        mvc.perform(get("/api/v1/grades/results/mine").param("period", "WF_SV").session(session("wf.sv1")))
                .andExpect(jsonPath("$[0].score").value(6.75));
    }

    @Test
    void completeWorkflowLocksScoresAndKeepsResultsPrivate() throws Exception {
        String group = registered();
        String council = council();
        ok("wf.gv0", "/dean/topics/WF_DT/reviewer", "teacher", "WF_GV2");
        ok("wf.gv0", "/dean/topics/WF_DT/councils", "council", council);
        upload("wf.sv1", group, "aaaaaaaaaaaaaaaaaaaaaaaa")
                .andExpect(flash().attributeExists("thanhCong"));
        // Browser retry of the same successful request does not create another version/object.
        upload("wf.sv1", group, "aaaaaaaaaaaaaaaaaaaaaaaa")
                .andExpect(flash().attributeExists("thanhCong"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM BAO_CAO WHERE Ma_Nhom=?",
                                Integer.class,
                                group))
                .isEqualTo(1);
        mvc.perform(get("/reports/aaaaaaaaaaaaaaaaaaaaaaaa/download").session(session("wf.sv1")))
                .andExpect(status().isOk());
        mvc.perform(get("/reports/aaaaaaaaaaaaaaaaaaaaaaaa/download").session(session("wf.sv2")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/reports/aaaaaaaaaaaaaaaaaaaaaaaa/download").session(session("wf.gv6")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/reports/aaaaaaaaaaaaaaaaaaaaaaaa/download").session(session("wf.gv1")))
                .andExpect(status().isOk());
        ok(
                "wf.gv2",
                "/lecturer/grading",
                "topic",
                "WF_DT",
                "type",
                "GVPB",
                "value",
                "9.00",
                "comment",
                "Review");
        upload("wf.sv1", group, "bbbbbbbbbbbbbbbbbbbbbbbb")
                .andExpect(flash().attributeExists("loi"));
        ok(
                "wf.gv3",
                "/lecturer/grading",
                "topic",
                "WF_DT",
                "type",
                "HD",
                "value",
                "6",
                "comment",
                "Chair");
        ok(
                "wf.gv4",
                "/lecturer/grading",
                "topic",
                "WF_DT",
                "type",
                "HD",
                "value",
                "6",
                "comment",
                "Secretary");
        action("wf.gv3", "/lecturer/topics/WF_DT/aggregate", "comment", "Summary")
                .andExpect(flash().attributeExists("loi"));
        ok(
                "wf.gv5",
                "/lecturer/grading",
                "topic",
                "WF_DT",
                "type",
                "HD",
                "value",
                "6",
                "comment",
                "Member");
        action("wf.gv4", "/lecturer/topics/WF_DT/aggregate", "comment", "Summary")
                .andExpect(status().isForbidden());
        ok("wf.gv3", "/lecturer/topics/WF_DT/aggregate", "comment", "Summary");
        em.clear();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Diem_Cuoi FROM KET_QUA_DE_TAI WHERE Ma_De_Tai='WF_DT'",
                                BigDecimal.class))
                .isEqualByComparingTo("6.75");
        mvc.perform(get("/student/results").session(session("wf.sv1")))
                .andExpect(model().attribute("results", org.hamcrest.Matchers.empty()));
        mvc.perform(get("/dean/approvals").session(session("wf.gv0")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("topics", org.hamcrest.Matchers.empty()))
                .andExpect(model().attribute("results", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/dean/topics/WF_DT/publish")));
        mvc.perform(get("/dean/approvals").param("period", "WF_GV").session(session("wf.gv0")))
                .andExpect(model().attribute("results", org.hamcrest.Matchers.empty()));
        mvc.perform(get("/dean/approvals").param("q", "no matching topic").session(session("wf.gv0")))
                .andExpect(model().attribute("results", org.hamcrest.Matchers.empty()));
        ok("wf.gv0", "/dean/topics/WF_DT/publish");
        mvc.perform(get("/dean/approvals").param("state", "DaCongBo").session(session("wf.gv0")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("topics", org.hamcrest.Matchers.empty()))
                .andExpect(model().attribute("results", org.hamcrest.Matchers.empty()));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Trang_Thai_Cong_Bo FROM KET_QUA_DE_TAI WHERE"
                                        + " Ma_De_Tai='WF_DT'",
                                String.class))
                .isEqualTo("DaCongBo");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Ngay_Cong_Bo FROM KET_QUA_DE_TAI WHERE Ma_De_Tai='WF_DT'",
                                java.sql.Timestamp.class))
                .isNotNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM EMAIL_KET_QUA WHERE Ma_De_Tai='WF_DT' AND"
                                        + " Trang_Thai='PENDING'",
                                Integer.class))
                .isEqualTo(3);
        assertThat(
                        jdbc.queryForList(
                                "SELECT Nguoi_Nhan FROM EMAIL_KET_QUA WHERE Ma_De_Tai='WF_DT'",
                                String.class))
                .doesNotHaveDuplicates()
                .hasSize(3);
        action("wf.gv0", "/dean/topics/WF_DT/publish").andExpect(flash().attributeExists("loi"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM EMAIL_KET_QUA WHERE Ma_De_Tai='WF_DT'",
                                Integer.class))
                .isEqualTo(3);
        mvc.perform(get("/dean/result-emails").session(session("wf.sv1")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/dean/result-emails").session(session("wf.gv0")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("emails", org.hamcrest.Matchers.hasSize(3)));
        verifyEmailManagement();
        for (String student : List.of("wf.sv7", "wf.sv8"))
            mvc.perform(
                            get("/api/v1/grades/results/mine")
                                    .param("period", "WF_SV")
                                    .session(session(student)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].score").value(6.75));
        mvc.perform(
                        get("/api/v1/grades/results/mine")
                                .param("period", "WF_SV")
                                .session(session("wf.sv1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].period").value("WF_SV"));
        mvc.perform(
                        get("/api/v1/grades/results/mine")
                                .param("period", "WF_SV")
                                .session(session("wf.sv2")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/student/results").param("period", "WF_GV").session(session("wf.sv1")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/dean/approvals").session(session("wf.gv0")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "/dean/result-emails")));
        mvc.perform(get("/student/results").session(session("wf.sv1")))
                .andExpect(model().attribute("results", org.hamcrest.Matchers.hasSize(1)));
        mvc.perform(get("/student/results").session(session("wf.sv2")))
                .andExpect(model().attribute("results", org.hamcrest.Matchers.empty()));
        action(
                        "wf.gv2",
                        "/lecturer/grading",
                        "topic",
                        "WF_DT",
                        "type",
                        "GVPB",
                        "value",
                        "10",
                        "comment",
                        "Changed")
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loi"));
        okPages("wf.gv3", "/lecturer/grading", "/lecturer/reports");
        okPages("wf.sv1", "/student/groups", "/student/reports", "/student/results");
        okPages("wf.gv0", "/dean/approvals", "/dean/councils");
        var period = em.find(com.example.quanlydetai.entity.DotDangKy.class, "WF_SV");
        String extension =
                "{\"maDot\":\"WF_SV\",\"tenDotDangKy\":\"Renamed\",\"loai\":\"MonHoc\",\"beginGV\":\""
                        + period.getBeginGV()
                        + "\",\"endGV\":\""
                        + period.getEndGV()
                        + "\",\"beginSV\":\""
                        + period.getBeginSV()
                        + "\",\"endSV\":\""
                        + period.getEndSV().plusDays(1)
                        + "\"}";
        mvc.perform(
                        put("/api/v1/registration-periods/WF_SV")
                                .session(session("wf.gv0"))
                                .with(csrf())
                                .contentType("application/json")
                                .content(extension))
                .andExpect(status().isBadRequest());
    }

    private void verifyEmailManagement() throws Exception {
        String api = "/api/v1/result-emails";
        mvc.perform(get(api)).andExpect(status().isUnauthorized());
        for (String user : List.of("wf.sv1", "wf.gv1", "wf.admin"))
            mvc.perform(get(api).session(session(user))).andExpect(status().isForbidden());
        mvc.perform(
                        get(api).param("q", "wf_sv7")
                                .param("state", "pending")
                                .session(session("wf.gv0")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].student").value("WF_SV7"))
                .andExpect(jsonPath("$.content[0].body").doesNotExist())
                .andExpect(jsonPath("$.content[0].receipt").doesNotExist());
        var ids =
                jdbc.queryForList(
                        "SELECT Ma_Email FROM EMAIL_KET_QUA ORDER BY Ngay_Tao DESC, Ma_Email DESC",
                        String.class);
        for (int page = 0; page < 2; page++)
            mvc.perform(
                            get(api).param("page", "" + page)
                                    .param("size", "1")
                                    .session(session("wf.gv0")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(3))
                    .andExpect(jsonPath("$.content[0].id").value(ids.get(page)));
        mvc.perform(get(api).param("q", "WF_%").session(session("wf.gv0")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        for (String[] invalid :
                List.of(
                        new String[] {"state", "UNKNOWN"},
                        new String[] {"page", "-1"},
                        new String[] {"size", "101"},
                        new String[] {"q", "x".repeat(201)}))
            mvc.perform(get(api).param(invalid[0], invalid[1]).session(session("wf.gv0")))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mvc.perform(
                        get("/dean/result-emails")
                                .param("q", "WF_SV7")
                                .param("state", "PENDING")
                                .session(session("wf.gv0")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("emails", org.hamcrest.Matchers.hasSize(1)));
        String id = ids.getFirst();
        mvc.perform(post(api + "/" + id + "/retry").session(session("wf.gv0")))
                .andExpect(status().isForbidden());
        mvc.perform(post(api + "/" + id + "/retry").with(csrf()).session(session("wf.sv1")))
                .andExpect(status().isForbidden());
        mvc.perform(post(api + "/" + id + "/retry").with(csrf()).session(session("wf.gv0")))
                .andExpect(status().isBadRequest());
        jdbc.update(
                "UPDATE EMAIL_KET_QUA SET Trang_Thai='FAILED', So_Lan_Thu=5 WHERE Ma_Email=?", id);
        em.clear();
        mvc.perform(get(api).param("state", "FAILED").session(session("wf.gv0")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/dean/result-emails").param("state", "FAILED").session(session("wf.gv0")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thử gửi lại")));
        mvc.perform(post(api + "/" + id + "/retry").with(csrf()).session(session("wf.gv0")))
                .andExpect(status().isNoContent());
        em.flush();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT So_Lan_Thu FROM EMAIL_KET_QUA WHERE Ma_Email=?",
                                Integer.class,
                                id))
                .isZero();
        mvc.perform(post(api + "/" + id + "/retry").with(csrf()).session(session("wf.gv0")))
                .andExpect(status().isBadRequest());
        mvc.perform(post(api + "/MISSING/retry").with(csrf()).session(session("wf.gv0")))
                .andExpect(status().isBadRequest());
        var now = LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        jdbc.update(
                "UPDATE EMAIL_KET_QUA SET Trang_Thai='PROCESSING', So_Lan_Thu=5, Lan_Thu_Tiep=?"
                        + " WHERE Ma_Email=?",
                now.minusMinutes(1),
                ids.get(0));
        jdbc.update(
                "UPDATE EMAIL_KET_QUA SET Trang_Thai='PROCESSING', So_Lan_Thu=5, Lan_Thu_Tiep=?"
                        + " WHERE Ma_Email=?",
                now.plusMinutes(2),
                ids.get(1));
        em.clear();
        assertThat(emailRows.failExhausted(5, now)).isEqualTo(1);
        assertThat(emailRows.findById(ids.get(0)).orElseThrow().getState()).isEqualTo("FAILED");
        assertThat(emailRows.findById(ids.get(1)).orElseThrow().getState()).isEqualTo("PROCESSING");
        assertThat(
                        emailRows
                                .findFirstByStateInAndNextAttemptAtLessThanEqualOrderByCreatedAtAscIdAsc(
                                        List.of("PENDING", "PROCESSING"), now)
                                .orElseThrow()
                                .getId())
                .isEqualTo(ids.get(2));
    }

    @Test
    void invitationsRequireConsentAndEnforceCapacityAndOneGroupPerRound() throws Exception {
        String group = group("wf.sv1");
        action("wf.sv1", "/student/registrations", "group", group, "topic", "WF_DT")
                .andExpect(flash().attributeExists("loi"));
        ok("wf.sv1", "/student/groups/" + group + "/invitations", "student", "WF_SV2");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM THANH_VIEN_NHOM WHERE Ma_Nhom=?",
                                Integer.class,
                                group))
                .isEqualTo(1);
        String invite =
                jdbc.queryForObject(
                        "SELECT Ma_Loi_Moi FROM LOI_MOI_NHOM WHERE Ma_SV='WF_SV2'", String.class);
        action("wf.sv3", "/student/invitations/" + invite, "accept", "true")
                .andExpect(status().isForbidden());
        ok("wf.sv2", "/student/invitations/" + invite, "accept", "true");
        action("wf.sv1", "/student/registrations", "group", group, "topic", "WF_DT")
                .andExpect(flash().attributeExists("loi"));
        action("wf.sv2", "/student/groups", "period", "WF_SV", "name", "Second group")
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loi"));
        ok("wf.sv1", "/student/groups/" + group + "/invitations", "student", "WF_SV3");
        String third =
                jdbc.queryForObject(
                        "SELECT Ma_Loi_Moi FROM LOI_MOI_NHOM WHERE Ma_SV='WF_SV3'", String.class);
        ok("wf.sv3", "/student/invitations/" + third, "accept", "true");
        join(group, "wf.sv1", 4, 5);
        action("wf.sv1", "/student/groups/" + group + "/invitations", "student", "WF_SV6")
                .andExpect(flash().attributeExists("loi"));
        action("wf.sv2", "/student/registrations", "group", group, "topic", "WF_DT")
                .andExpect(status().isForbidden());
        ok("wf.sv1", "/student/registrations", "group", group, "topic", "WF_DT");
        action("wf.sv2", "/student/groups/" + group + "/members/remove", "student", "WF_SV2")
                .andExpect(flash().attributeExists("loi"));
    }

    @Test
    void leaderCanRenameCreatedGroupThroughHtmlAndApiWithoutChangingItsIdentity() throws Exception {
        String id = group("wf.sv1");
        join(id, "wf.sv1", 2, 3);
        mvc.perform(get("/student/groups").session(session("wf.sv1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chỉnh sửa tên nhóm")));
        action("wf.sv1", "/student/groups/" + id + "/name", "name", "  Nhóm đổi tên  ",
                        "period", "WF_GV", "topic", "WF_DT", "leader", "WF_SV2")
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("thanhCong"));
        mvc.perform(get("/api/v1/groups").session(session("wf.sv2")))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].name").value("Nhóm đổi tên"))
                .andExpect(jsonPath("$[0].members.length()").value(3));
        renameGroupApi("wf.sv1", id, "<b>Nhóm API</b>").andExpect(status().isNoContent());
        mvc.perform(get("/student/groups").session(session("wf.sv1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;b&gt;Nhóm API&lt;/b&gt;")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<b>Nhóm API</b>"))));
        var row = jdbc.queryForMap("SELECT Ma_Dot,Ma_De_Tai FROM NHOM WHERE Ma_Nhom=?", id);
        assertThat(row.get("Ma_Dot")).isEqualTo("WF_SV");
        assertThat(row.get("Ma_De_Tai")).isNull();
        assertThat(jdbc.queryForObject("SELECT Ma_SV FROM THANH_VIEN_NHOM WHERE Ma_Nhom=? AND Vai_Tro='Truong_Nhom'", String.class, id))
                .isEqualTo("WF_SV1");
    }

    @Test
    void groupRenameRequiresLeaderRoleAuthenticationAndCsrf() throws Exception {
        String id = group("wf.sv1");
        join(id, "wf.sv1", 2, 3);
        mvc.perform(patch("/api/v1/groups/" + id + "/name").with(csrf())
                        .contentType("application/json").content("{\"name\":\"Forged\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/groups/" + id + "/name").session(session("wf.sv1"))
                        .contentType("application/json").content("{\"name\":\"Forged\"}"))
                .andExpect(status().isForbidden());
        for (String user : List.of("wf.sv2", "wf.sv4", "wf.gv1")) {
            renameGroupApi(user, id, "Forged").andExpect(status().isForbidden());
            action(user, "/student/groups/" + id + "/name", "name", "Forged")
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/student/groups").session(session("wf.sv2")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Chỉnh sửa tên nhóm"))));
        assertThat(jdbc.queryForObject("SELECT Ten_Nhom FROM NHOM WHERE Ma_Nhom=?", String.class, id))
                .isEqualTo("Group wf.sv1");
    }

    @Test
    void groupRenameValidatesNamesAndPreservesTheCorrectEditFormOnError() throws Exception {
        String id = group("wf.sv1");
        for (String name : List.of("", "   ", "a".repeat(151))) {
            action("wf.sv1", "/student/groups/" + id + "/name", "name", name)
                    .andExpect(status().isOk())
                    .andExpect(view().name("sv/groups"))
                    .andExpect(model().attributeHasFieldErrors("groupEditForm", "name"))
                    .andExpect(model().attribute("editingGroupId", id))
                    .andExpect(model().attribute("groupEditForm", new com.example.quanlydetai.dto.CapNhatNhomRequest(name)))
                    .andExpect(model().attributeExists("groupEditError"));
            renameGroupApi("wf.sv1", id, name).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.name").exists());
        }
        assertThat(jdbc.queryForObject("SELECT Ten_Nhom FROM NHOM WHERE Ma_Nhom=?", String.class, id))
                .isEqualTo("Group wf.sv1");
    }

    @Test
    void groupRenameLocksForPendingAndAcceptedRegistrationAndReopensAfterRejectionOrWithdrawal() throws Exception {
        String registration = pendingRegistration();
        String id = jdbc.queryForObject("SELECT Ma_Nhom FROM DANG_KY_DE_TAI WHERE Ma_DK=?", String.class, registration);
        String locked = "Không được sửa tên nhóm khi đăng ký đang chờ duyệt hoặc đã được chấp thuận.";
        action("wf.sv1", "/student/groups/" + id + "/name", "name", "Blocked")
                .andExpect(model().attribute("loi", locked));
        renameGroupApi("wf.sv1", id, "Blocked").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(locked));
        mvc.perform(get("/student/groups").session(session("wf.sv1")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Chỉnh sửa tên nhóm"))));
        ok("wf.gv0", "/dean/registrations/" + registration + "/review", "accept", "false", "reason", "Need revisions");
        ok("wf.sv1", "/student/groups/" + id + "/name", "name", "After rejection");
        ok("wf.sv1", "/student/registrations", "group", id, "topic", "WF_DT");
        registration = jdbc.queryForObject("SELECT Ma_DK FROM DANG_KY_DE_TAI WHERE Active_Group=?", String.class, id);
        ok("wf.sv1", "/student/registrations/" + registration + "/withdraw");
        renameGroupApi("wf.sv1", id, "After withdrawal").andExpect(status().isNoContent());
        ok("wf.sv1", "/student/registrations", "group", id, "topic", "WF_DT");
        registration = jdbc.queryForObject("SELECT Ma_DK FROM DANG_KY_DE_TAI WHERE Active_Group=?", String.class, id);
        ok("wf.gv0", "/dean/registrations/" + registration + "/review", "accept", "true", "reason", "");
        renameGroupApi("wf.sv1", id, "Blocked again").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(locked));
        assertThat(jdbc.queryForObject("SELECT Ten_Nhom FROM NHOM WHERE Ma_Nhom=?", String.class, id))
                .isEqualTo("After withdrawal");
        assertThat(jdbc.queryForObject("SELECT Ma_De_Tai FROM NHOM WHERE Ma_Nhom=?", String.class, id))
                .isEqualTo("WF_DT");
    }

    @Test
    void groupRenameIsUnavailableWhenStudentRegistrationWindowCloses() throws Exception {
        String id = group("wf.sv1");
        jdbc.update("UPDATE DOT_DANG_KY SET End_SV=? WHERE Ma_Dot='WF_SV'", LocalDateTime.now().minusDays(1));
        em.clear();
        action("wf.sv1", "/student/groups/" + id + "/name", "name", "Too late")
                .andExpect(model().attribute("loi", "Đã ngoài thời gian sinh viên đăng ký."));
        renameGroupApi("wf.sv1", id, "Too late").andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/groups").session(session("wf.sv1")))
                .andExpect(jsonPath("$[0].editable").value(false));
        mvc.perform(get("/student/groups").session(session("wf.sv1")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Chỉnh sửa tên nhóm"))));
        assertThat(jdbc.queryForObject("SELECT Ten_Nhom FROM NHOM WHERE Ma_Nhom=?", String.class, id))
                .isEqualTo("Group wf.sv1");
    }

    private ResultActions renameGroupApi(String user, String id, String name) throws Exception {
        return mvc.perform(patch("/api/v1/groups/" + id + "/name").session(session(user)).with(csrf())
                .contentType("application/json").content("{\"name\":\"" + name + "\"}"));
    }

    @Test
    void rejectingOrWithdrawingReleasesTopicWithoutSelectingIt() throws Exception {
        String g1 = group("wf.sv1"), g2 = group("wf.sv2");
        join(g1, "wf.sv1", 7, 8);
        join(g2, "wf.sv2", 9, 10);
        ok("wf.sv1", "/student/registrations", "group", g1, "topic", "WF_DT");
        action("wf.sv2", "/student/registrations", "group", g2, "topic", "WF_DT")
                .andExpect(flash().attributeExists("loi"));
        String id =
                jdbc.queryForObject(
                        "SELECT Ma_DK FROM DANG_KY_DE_TAI WHERE Active_Group=?", String.class, g1);
        ok(
                "wf.gv0",
                "/dean/registrations/" + id + "/review",
                "accept",
                "false",
                "reason",
                "Need revisions");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Ma_De_Tai FROM NHOM WHERE Ma_Nhom=?", String.class, g1))
                .isNull();
        ok("wf.sv2", "/student/registrations", "group", g2, "topic", "WF_DT");
        String second =
                jdbc.queryForObject(
                        "SELECT Ma_DK FROM DANG_KY_DE_TAI WHERE Active_Group=?", String.class, g2);
        ok("wf.sv2", "/student/registrations/" + second + "/withdraw");
        ok("wf.sv1", "/student/registrations", "group", g1, "topic", "WF_DT");
    }

    @Test
    void supervisorAndIndependentReviewerCannotBeCouncilMembers() throws Exception {
        registered();
        String h = council();
        action("wf.gv0", "/dean/topics/WF_DT/reviewer", "teacher", "WF_GV1")
                .andExpect(flash().attributeExists("loi"));
        ok("wf.gv0", "/dean/topics/WF_DT/reviewer", "teacher", "WF_GV3");
        action("wf.gv0", "/dean/topics/WF_DT/councils", "council", h)
                .andExpect(flash().attributeExists("loi"));
        action(
                        "wf.gv1",
                        "/lecturer/grading",
                        "topic",
                        "WF_DT",
                        "type",
                        "HD",
                        "value",
                        "8",
                        "comment",
                        "Own supervision")
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loi"));
    }

    @Test
    void topicSubmissionUsesCurrentTeacherAndTimeWindow() throws Exception {
        ok(
                "wf.gv1",
                "/lecturer/topics",
                "period",
                "WF_GV",
                "title",
                "New proposal",
                "content",
                "Details",
                "department",
                "WF_BM",
                "maGvDangKy",
                "WF_GV2");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Ma_GV_Dang_Ky FROM DE_TAI WHERE Ten_De_Tai='New proposal'",
                                String.class))
                .isEqualTo("WF_GV1");
        action(
                        "wf.gv1",
                        "/lecturer/topics",
                        "period",
                        "WF_SV",
                        "title",
                        "Too late",
                        "content",
                        "Details",
                        "department",
                        "WF_BM")
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loi"));
        mvc.perform(get("/lecturer/topics").session(session("wf.gv1"))).andExpect(status().isOk());
    }

    @Test
    void scoreValidationRejectsOutOfRangeAndExcessPrecision() throws Exception {
        for (String value : List.of("-1", "10.01", "6.123"))
            action(
                            "wf.gv2",
                            "/lecturer/grading",
                            "topic",
                            "WF_DT",
                            "type",
                            "GVPB",
                            "value",
                            value,
                            "comment",
                            "Review")
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("scoreForm", "value"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM DIEM_THANH_PHAN WHERE Ma_De_Tai='WF_DT'",
                                Integer.class))
                .isZero();
    }

    @Test
    void tlcnReviewerDeadlineBlocksLateNewAndChangedScores() throws Exception {
        verifyReviewerDeadline("TLCN");
    }

    @Test
    void kltnReviewerDeadlineBlocksLateNewAndChangedScores() throws Exception {
        verifyReviewerDeadline("KLTN");
    }

    private void verifyReviewerDeadline(String type) throws Exception {
        String group = registered();
        ok("wf.gv0", "/dean/topics/WF_DT/reviewer", "teacher", "WF_GV2");
        ok("wf.gv0", "/dean/topics/WF_DT/councils", "council", council());
        upload("wf.sv1", group, "dddddddddddddddddddddddd")
                .andExpect(flash().attributeExists("thanhCong"));
        var now = LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        jdbc.update(
                "UPDATE DOT_DANG_KY SET Loai=?, Deadline_GVPB_Nop_Diem=?, Time_BC_HD=? WHERE"
                        + " Ma_Dot='WF_SV'",
                type,
                now.minusDays(1),
                "KLTN".equals(type) ? now.plusDays(1) : null);
        em.clear();
        action(
                        "wf.gv2",
                        "/lecturer/grading",
                        "topic",
                        "WF_DT",
                        "type",
                        "GVPB",
                        "value",
                        "8",
                        "comment",
                        "Review")
                .andExpect(model().attributeExists("loi"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM DIEM_THANH_PHAN WHERE Ma_De_Tai='WF_DT'",
                                Integer.class))
                .isZero();
        jdbc.update(
                "UPDATE DOT_DANG_KY SET Deadline_GVPB_Nop_Diem=? WHERE Ma_Dot='WF_SV'",
                now.plusDays(1));
        em.clear();
        ok(
                "wf.gv2",
                "/lecturer/grading",
                "topic",
                "WF_DT",
                "type",
                "GVPB",
                "value",
                "9",
                "comment",
                "Review");
        jdbc.update(
                "UPDATE DOT_DANG_KY SET Deadline_GVPB_Nop_Diem=? WHERE Ma_Dot='WF_SV'",
                now.minusDays(1));
        em.clear();
        mvc.perform(
                        put("/api/v1/grades/ballots")
                                .session(session("wf.gv2"))
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        "{\"topic\":\"WF_DT\",\"type\":\"GVPB\",\"value\":8,\"comment\":\"Late"
                                            + " edit\"}"))
                .andExpect(status().isBadRequest());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Diem FROM DIEM_THANH_PHAN WHERE Ma_De_Tai='WF_DT'",
                                BigDecimal.class))
                .isEqualByComparingTo("9");
    }

    @Test
    void officialNoticesRespectScopeAndRenderContentAsText() throws Exception {
        action("wf.gv1", "/announcements", "title", "Bad", "content", "Bad", "scope", "Khoa")
                .andExpect(status().isForbidden());
        action("wf.gv0", "/announcements", "title", "Bad", "content", "Bad", "scope", "NhaTruong")
                .andExpect(status().isForbidden());
        ok(
                "wf.gv0",
                "/announcements",
                "title",
                "Faculty notice",
                "content",
                "<script>alert(1)</script>",
                "scope",
                "Khoa");
        mvc.perform(get("/announcements").session(session("wf.sv1")))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;")));
    }

    @Test
    void passwordResetExpiresOldSessionAndDoesNotExposeHash() throws Exception {
        var old = session("wf.sv1");
        ok("wf.admin", "/admin/accounts/wf.sv1/password", "password", "Updated-Test-Password!");
        em.clear();
        mvc.perform(get("/").session(old)).andExpect(redirectedUrl("/login?expired"));
        mvc.perform(formLogin().user("wf.sv1").password("Updated-Test-Password!"))
                .andExpect(authenticated());
        mvc.perform(get("/admin/accounts").session(session("wf.admin")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(hash))));
    }

    @Test
    void everyRoleCanRenderAllOfItsPages() throws Exception {
        okPages(
                "wf.sv1",
                "/",
                "/student/topics",
                "/student/groups",
                "/student/reports",
                "/student/results",
                "/announcements",
                "/accounts/password");
        okPages("wf.gv1", "/lecturer/topics", "/lecturer/grading", "/lecturer/reports");
        okPages("wf.gv0", "/dean/periods", "/dean/approvals", "/dean/councils");
        okPages("wf.admin", "/admin/accounts");
    }

    @Test
    void arithmeticMeansIndividualBallotsAndRoundsOnce() {
        assertThat(
                        DiemService.average(
                                List.of(
                                        new BigDecimal("9"),
                                        new BigDecimal("6"),
                                        new BigDecimal("6"),
                                        new BigDecimal("6"))))
                .isEqualByComparingTo("6.75");
        assertThat(DiemService.average(List.of(BigDecimal.ZERO, new BigDecimal("0.01"))))
                .isEqualByComparingTo("0.01");
    }

    @Test
    void submittedOnTimeCanBeApprovedAfterRegistrationWindowCloses() throws Exception {
        String group = group("wf.sv1");
        join(group, "wf.sv1", 7, 8);
        ok("wf.sv1", "/student/registrations", "group", group, "topic", "WF_DT");
        String id =
                jdbc.queryForObject(
                        "SELECT Ma_DK FROM DANG_KY_DE_TAI WHERE Active_Group=?",
                        String.class,
                        group);
        jdbc.update(
                "UPDATE DANG_KY_DE_TAI SET Ngay_Dang_Ky=? WHERE Ma_DK=?",
                LocalDateTime.now().minusHours(2),
                id);
        jdbc.update(
                "UPDATE DOT_DANG_KY SET End_SV=? WHERE Ma_Dot='WF_SV'",
                LocalDateTime.now().minusHours(1));
        em.clear();
        ok("wf.gv0", "/dean/registrations/" + id + "/review", "accept", "true", "reason", "");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Ma_De_Tai FROM NHOM WHERE Ma_Nhom=?", String.class, group))
                .isEqualTo("WF_DT");
    }

    @Test
    void changingPasswordRequiresCurrentPasswordAndInvalidatesSession() throws Exception {
        session("wf.sv1");
        action(
                        "wf.sv1",
                        "/accounts/password",
                        "currentPassword",
                        "Wrong-password!",
                        "newPassword",
                        "Changed-Password!",
                        "confirmation",
                        "Changed-Password!")
                .andExpect(flash().attributeExists("loi"));
        action(
                        "wf.sv1",
                        "/accounts/password",
                        "currentPassword",
                        "Workflow-Test-Password!",
                        "newPassword",
                        "Changed-Password!",
                        "confirmation",
                        "Changed-Password!")
                .andExpect(redirectedUrl("/login?passwordChanged"));
        mvc.perform(formLogin().user("wf.sv1").password("Workflow-Test-Password!"))
                .andExpect(unauthenticated());
        mvc.perform(formLogin().user("wf.sv1").password("Changed-Password!"))
                .andExpect(authenticated());
    }

    @Test
    void adminCreatesStudentAndCannotLockItself() throws Exception {
        ok(
                "wf.admin",
                "/admin/accounts",
                "username",
                "wf.newstudent",
                "password",
                "New-Student-Password!",
                "role",
                "SV",
                "profileId",
                "WF_NEW_SV",
                "name",
                "New student",
                "email",
                "newstudent@example.test");
        mvc.perform(formLogin().user("wf.newstudent").password("New-Student-Password!"))
                .andExpect(authenticated());
        action("wf.admin", "/admin/accounts/wf.admin/status", "active", "false")
                .andExpect(flash().attributeExists("loi"));
        action("wf.sv1", "/admin/accounts/wf.gv1/status", "active", "false")
                .andExpect(status().isForbidden());
    }

    @Test
    void deanRevocationPreservesPeriodHistoryAndRemovesLiveAuthority() throws Exception {
        var old = session("wf.gv0");
        ok("wf.admin", "/admin/dean", "teacher", "WF_GV0", "appointed", "false");
        em.clear();
        mvc.perform(get("/dean/periods").session(old)).andExpect(status().isForbidden());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Ma_Truong_Khoa FROM DOT_DANG_KY WHERE Ma_Dot='WF_SV'",
                                String.class))
                .isEqualTo("WF_GV0");
    }

    @Test
    void reviewerAssignmentRejectsLockedAndAccountlessTeachersWithoutReplacingPrevious()
            throws Exception {
        registered();
        ok("wf.gv0", "/dean/topics/WF_DT/reviewer", "teacher", "WF_GV2");
        jdbc.update("UPDATE TAI_KHOAN SET Trang_Thai='Khoa' WHERE Ma_TK='wf.gv6'");
        jdbc.update(
                "INSERT INTO GV(Ma_GV,Ho_Ten,Email,Ma_Bo_Mon) VALUES ('WF_ORPHAN','No"
                        + " account','orphan@example.test','WF_BM')");
        em.clear();
        for (String teacher : List.of("WF_GV6", "WF_ORPHAN")) {
            mvc.perform(
                            put("/api/v1/councils/topics/WF_DT/reviewer")
                                    .session(session("wf.gv0"))
                                    .with(csrf())
                                    .contentType("application/json")
                                    .content("{\"teacher\":\"" + teacher + "\"}"))
                    .andExpect(status().isBadRequest());
            assertThat(
                            jdbc.queryForList(
                                    "SELECT Ma_GV FROM GVPB_DETAI WHERE Ma_De_Tai='WF_DT'",
                                    String.class))
                    .containsExactly("WF_GV2");
        }
        jdbc.update("UPDATE TAI_KHOAN SET Trang_Thai='HoatDong' WHERE Ma_TK='wf.gv6'");
        em.clear();
        ok("wf.gv0", "/dean/topics/WF_DT/reviewer", "teacher", "WF_GV6");
        assertThat(
                        jdbc.queryForList(
                                "SELECT Ma_GV FROM GVPB_DETAI WHERE Ma_De_Tai='WF_DT'",
                                String.class))
                .containsExactly("WF_GV6");
    }

    @Test
    void councilAssignmentRechecksAccountsEvenAfterFinalization() throws Exception {
        registered();
        String id = council();
        jdbc.update("UPDATE TAI_KHOAN SET Trang_Thai='Khoa' WHERE Ma_TK='wf.gv4'");
        em.clear();
        mvc.perform(
                        put("/api/v1/councils/topics/WF_DT")
                                .session(session("wf.gv0"))
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"council\":\"" + id + "\"}"))
                .andExpect(status().isBadRequest());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM HOI_DONG_DE_TAI WHERE Ma_De_Tai='WF_DT'",
                                Integer.class))
                .isZero();
        jdbc.update("UPDATE TAI_KHOAN SET Trang_Thai='HoatDong' WHERE Ma_TK='wf.gv4'");
        em.clear();
        ok("wf.gv0", "/dean/topics/WF_DT/councils", "council", id);
    }

    @Test
    void lockImpactTracksMissingBallotsAndChairAggregationButNotCompletedTasks() throws Exception {
        String group = registered();
        String id = council();
        ok("wf.gv0", "/dean/topics/WF_DT/reviewer", "teacher", "WF_GV2");
        ok("wf.gv0", "/dean/topics/WF_DT/councils", "council", id);
        upload("wf.sv1", group, "cccccccccccccccccccccccc")
                .andExpect(flash().attributeExists("thanhCong"));
        for (int teacher = 2; teacher <= 5; teacher++) {
            mvc.perform(
                            get("/api/v1/admin/accounts/wf.gv" + teacher + "/lock-impact")
                                    .session(session("wf.admin")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tasks.length()").value(1));
            ok(
                    "wf.gv" + teacher,
                    "/lecturer/grading",
                    "topic",
                    "WF_DT",
                    "type",
                    teacher == 2 ? "GVPB" : "HD",
                    "value",
                    "8",
                    "comment",
                    "Đã chấm");
        }
        for (int teacher : List.of(2, 4, 5)) {
            mvc.perform(
                            get("/api/v1/admin/accounts/wf.gv" + teacher + "/lock-impact")
                                    .session(session("wf.admin")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tasks.length()").value(0));
        }
        // A chair who has submitted their ballot still has to aggregate the result.
        mvc.perform(get("/api/v1/admin/accounts/wf.gv3/lock-impact").session(session("wf.admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks.length()").value(1));
        ok("wf.gv3", "/lecturer/topics/WF_DT/aggregate", "comment", "Hoàn tất đánh giá");
        for (int teacher : List.of(1, 2, 3, 4, 5)) {
            mvc.perform(
                            get("/api/v1/admin/accounts/wf.gv" + teacher + "/lock-impact")
                                    .session(session("wf.admin")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tasks.length()").value(0));
        }
    }

    private void okPages(String user, String... paths) throws Exception {
        for (String path : paths)
            mvc.perform(get(path).session(session(user))).andExpect(status().isOk());
    }
}
