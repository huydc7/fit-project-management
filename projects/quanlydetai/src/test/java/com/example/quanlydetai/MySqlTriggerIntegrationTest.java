package com.example.quanlydetai;

import static org.assertj.core.api.Assertions.*;

import com.example.quanlydetai.entity.HoiDong;
import com.example.quanlydetai.exception.DatabaseErrorMessages;
import com.example.quanlydetai.repository.HoiDongRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Requires scripts 01-09 on an isolated MySQL database. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "TEST_DB_DRIVER", matches = "com.mysql.cj.jdbc.Driver")
class MySqlTriggerIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired HoiDongRepository councils;

    private void seedGroup() {
        jdbc.update("INSERT INTO BO_MON VALUES ('SIZE_BM','Size test')");
        jdbc.update(
                "INSERT INTO GV(Ma_GV,Ho_Ten,Email,Ma_Bo_Mon) VALUES"
                        + " ('SIZE_GV','Teacher','size@example.test','SIZE_BM')");
        jdbc.update("INSERT INTO TRUONG_KHOA(Ma_GV,Dang_Giu_Chuc) VALUES ('SIZE_GV',true)");
        jdbc.update(
                "INSERT INTO DOT_DANG_KY VALUES ('SIZE_DOT','SIZE_GV','Size"
                    + " period','MonHoc',NOW()-INTERVAL 10 DAY,NOW()-INTERVAL 5 DAY,NOW()+INTERVAL"
                    + " 1 DAY,NOW()+INTERVAL 10 DAY,NULL,NULL)");
        jdbc.update(
                "INSERT INTO"
                    + " DE_TAI(Ma_De_Tai,Ten_De_Tai,Ma_Bo_Mon,Noi_Dung,Trang_Thai,Ma_Dot,Ma_GV_Dang_Ky,Ngay_Dang_Ky)"
                    + " VALUES ('SIZE_DT','Size"
                    + " topic','SIZE_BM','Content','Nhap','SIZE_DOT','SIZE_GV',NOW()-INTERVAL 6"
                    + " DAY)");
        jdbc.update("INSERT INTO GVHD_DETAI VALUES ('SIZE_GV','SIZE_DT')");
        jdbc.update("UPDATE DE_TAI SET Trang_Thai='DaCongBo' WHERE Ma_De_Tai='SIZE_DT'");
        jdbc.update("UPDATE DOT_DANG_KY SET Begin_SV=NOW()-INTERVAL 4 DAY WHERE Ma_Dot='SIZE_DOT'");
        jdbc.update(
                "INSERT INTO NHOM(Ma_Nhom,Ten_Nhom,Ma_Dot) VALUES ('SIZE_N','Size"
                        + " group','SIZE_DOT')");
        for (int i = 1; i <= 6; i++) {
            jdbc.update(
                    "INSERT INTO SV(Ma_SV,Ho_Ten,Email) VALUES (?,?,?)",
                    "SIZE_SV" + i,
                    "Student " + i,
                    "size" + i + "@example.test");
        }
    }

    private void addMember(int i) {
        jdbc.update(
                "INSERT INTO THANH_VIEN_NHOM(Ma_Nhom,Ma_SV,Vai_Tro) VALUES ('SIZE_N',?,?)",
                "SIZE_SV" + i,
                i == 1 ? "Truong_Nhom" : "Thanh_Vien");
    }

    @Test
    void mysqlRejectsTwoMembersButAcceptsThreeOnTopicSelection() {
        seedGroup();
        addMember(1);
        addMember(2);
        Throwable failure =
                catchThrowable(
                        () ->
                                jdbc.update(
                                        "UPDATE NHOM SET Ma_De_Tai='SIZE_DT' WHERE"
                                                + " Ma_Nhom='SIZE_N'"));
        assertThat(failure).isNotNull();
        assertThat(DatabaseErrorMessages.extractRootCauseMessage(failure)).contains("3-5 SV");
        addMember(3);
        assertThat(jdbc.update("UPDATE NHOM SET Ma_De_Tai='SIZE_DT' WHERE Ma_Nhom='SIZE_N'"))
                .isEqualTo(1);
    }

    @Test
    void mysqlAcceptsFiveMembersButRejectsSixth() {
        seedGroup();
        for (int i = 1; i <= 5; i++) addMember(i);
        Throwable failure = catchThrowable(() -> addMember(6));
        assertThat(failure).isNotNull();
        assertThat(DatabaseErrorMessages.extractRootCauseMessage(failure)).contains("tối đa 5");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM THANH_VIEN_NHOM WHERE Ma_Nhom='SIZE_N'",
                                Integer.class))
                .isEqualTo(5);
    }

    @Test
    void mysqlRejectsLateTopicPublicationAndDepartmentApproval() {
        seedGroup();
        jdbc.update(
                "INSERT INTO"
                    + " DE_TAI(Ma_De_Tai,Ten_De_Tai,Ma_Bo_Mon,Noi_Dung,Trang_Thai,Ma_Dot,Ma_GV_Dang_Ky,Ngay_Dang_Ky)"
                    + " VALUES"
                    + " ('SIZE_LATE','Late','SIZE_BM','Content','Nhap','SIZE_DOT','SIZE_GV',NOW()-INTERVAL"
                    + " 6 DAY)");
        jdbc.update("INSERT INTO GVHD_DETAI VALUES ('SIZE_GV','SIZE_LATE')");
        var publication =
                catchThrowable(
                        () ->
                                jdbc.update(
                                        "UPDATE DE_TAI SET Trang_Thai='DaCongBo' WHERE"
                                                + " Ma_De_Tai='SIZE_LATE'"));
        assertThat(publication).isNotNull();
        assertThat(DatabaseErrorMessages.extractRootCauseMessage(publication))
                .contains("Chi cong bo");
        var approval =
                catchThrowable(
                        () ->
                                jdbc.update(
                                        "INSERT INTO DUYET_BO_MON(Ma_De_Tai,Ma_GV,Ngay_Duyet)"
                                                + " VALUES ('SIZE_LATE','SIZE_GV',NOW())"));
        assertThat(approval).isNotNull();
        assertThat(DatabaseErrorMessages.extractRootCauseMessage(approval))
                .contains("Chi duyet");
    }

    @Test
    void mysqlDepartmentApprovalRequiresSupervisorsAndFreezesTheirList() {
        jdbc.update("INSERT INTO BO_MON VALUES ('ASSIGN_BM','Assignment department')");
        jdbc.update("INSERT INTO GV(Ma_GV,Ho_Ten,Email,Ma_Bo_Mon) VALUES "
                + "('ASSIGN_HEAD','Head','head@example.test','ASSIGN_BM'),"
                + "('ASSIGN_GV','Teacher','teacher@example.test','ASSIGN_BM')");
        jdbc.update("INSERT INTO TRUONG_KHOA VALUES ('ASSIGN_HEAD',true)");
        jdbc.update("INSERT INTO TRUONG_BO_MON VALUES ('ASSIGN_BM','ASSIGN_HEAD',true)");
        jdbc.update("INSERT INTO DOT_DANG_KY VALUES ('ASSIGN_DOT','ASSIGN_HEAD','Assignment','MonHoc',"
                + "'2099-01-01','2099-01-02','2099-01-03','2099-01-04',NULL,NULL)");
        jdbc.update("INSERT INTO DE_TAI(Ma_De_Tai,Ten_De_Tai,Noi_Dung,Ma_Bo_Mon,Ma_Dot,Ma_GV_Dang_Ky,Trang_Thai,Ngay_Dang_Ky) "
                + "VALUES ('ASSIGN_DT','Assignment','Content','ASSIGN_BM','ASSIGN_DOT','ASSIGN_GV','Nhap','2099-01-01 01:00:00')");
        String approve = "INSERT INTO DUYET_BO_MON VALUES ('ASSIGN_DT','ASSIGN_HEAD',NOW())";
        Throwable missing = catchThrowable(() -> jdbc.update(approve));
        assertThat(DatabaseErrorMessages.extractRootCauseMessage(missing)).contains("1-2 GVHD");
        jdbc.update("INSERT INTO GVHD_DETAI VALUES ('ASSIGN_GV','ASSIGN_DT')");
        jdbc.update(approve);
        Throwable change = catchThrowable(() -> jdbc.update("DELETE FROM GVHD_DETAI WHERE Ma_De_Tai='ASSIGN_DT'"));
        assertThat(DatabaseErrorMessages.extractRootCauseMessage(change)).contains("Huy xac nhan");
        jdbc.update("DELETE FROM DUYET_BO_MON WHERE Ma_De_Tai='ASSIGN_DT'");
        assertThat(jdbc.update("DELETE FROM GVHD_DETAI WHERE Ma_De_Tai='ASSIGN_DT'")).isEqualTo(1);
    }

    @Test
    void actualMysqlSignalSurvivesHibernateWrapping() {
        jdbc.update("INSERT INTO BO_MON VALUES ('TRIGGER_BM','Trigger department')");
        jdbc.update(
                "INSERT INTO GV VALUES ('TRIGGER_GV','Trigger"
                        + " teacher','trigger@example.test',NULL,'TRIGGER_BM')");
        jdbc.update("INSERT INTO TRUONG_KHOA(Ma_GV,Dang_Giu_Chuc) VALUES ('TRIGGER_GV',true)");
        jdbc.update(
                "INSERT INTO DOT_DANG_KY VALUES ('TRIGGER_DOT','TRIGGER_GV','Trigger"
                        + " period','MonHoc',"
                        + " '2099-01-01','2099-01-02','2099-01-03','2099-01-04',NULL,NULL)");
        HoiDong council = new HoiDong();
        council.setMaHd("TRIGGER_HD");
        council.setMaDot("TRIGGER_DOT");
        council.setTrangThai("DangLap");
        council = councils.saveAndFlush(council);
        council.setTrangThai("DaChot");
        Throwable failure = catchThrowable(() -> councils.flush());
        assertThat(failure).isNotNull();
        assertThat(DatabaseErrorMessages.extractRootCauseMessage(failure))
                .isEqualTo("Hội đồng phải có 3-5 GV, đúng 1 Chủ tịch và 1 Thư ký.");
    }
}
