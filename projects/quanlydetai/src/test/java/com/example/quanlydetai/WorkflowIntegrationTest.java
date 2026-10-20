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
                mapper.readTree(anonymous.getResponse().getContentAsString()).get("token").asText();
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
                mapper.readTree(csrf.getResponse().getContentAsString()).get("token").asText();
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
        ok(
                "wf.gv0",
                "/dean/councils",
                "period",
                "WF_SV",
                "chair",
                "WF_GV3",
                "secretary",
                "WF_GV4",
                "members",
                "WF_GV5");
        String id =
                jdbc.queryForObject(
                        "SELECT Ma_HD FROM HOI_DONG WHERE Ma_Dot='WF_SV'", String.class);
        ok("wf.gv0", "/dean/councils/" + id + "/finalize");
        return id;
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
        ok("wf.gv0", "/dean/topics/WF_DT/publish");
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
        var old = session("wf.sv1");
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

    private void okPages(String user, String... paths) throws Exception {
        for (String path : paths)
            mvc.perform(get(path).session(session(user))).andExpect(status().isOk());
    }
}
