package com.example.quanlydetai.tools.sample;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:faculty-sample;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.hikari.connection-init-sql=",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "app.sample-data.enabled=true",
            "app.sample-data.refresh-existing=true",
            "app.sample-data.password=Integration-Sample-Only!",
            "app.sample-data.path=../Database/sample-data/fit-hcmute.json"
        })
@org.springframework.context.annotation.Import(SampleDataInitializer.class)
@ActiveProfiles("dev")
@Transactional
class SampleDataIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired SampleDataInitializer importer;

    @Test
    void importsRelevantFacultyCatalogAndAllFourPeriodTypes() {
        assertThat(count("BO_MON")).isEqualTo(4);
        assertThat(count("GV")).isEqualTo(9);
        assertThat(count("SV")).isEqualTo(18);
        assertThat(count("TAI_KHOAN")).isEqualTo(28);
        assertThat(count("DOT_DANG_KY")).isEqualTo(5);
        assertThat(count("DE_TAI")).isEqualTo(12);
        assertThat(jdbc.queryForList("SELECT DISTINCT Loai FROM DOT_DANG_KY", String.class))
                .containsExactlyInAnyOrder("MonHoc", "TLCN", "KLTN", "NCKH");
        assertThat(jdbc.queryForList("SELECT Ma_Bo_Mon FROM BO_MON", String.class))
                .containsExactlyInAnyOrder("CNPM", "HTTT", "MANG_ANM", "TTNT");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM DE_TAI WHERE Ma_De_Tai LIKE 'DEMO_%'",
                                Long.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM DOT_DANG_KY WHERE Loai='KLTN' AND"
                                    + " Deadline_GVPB_Nop_Diem IS NOT NULL AND Time_BC_HD IS NOT"
                                    + " NULL",
                                Long.class))
                .isEqualTo(1);
    }

    @Test
    void refreshPreservesCredentialsAccountStatusDatesAndUserRecords() throws Exception {
        String originalHash =
                jdbc.queryForObject(
                        "SELECT Mat_Khau_Hash FROM TAI_KHOAN WHERE Ten_Dang_Nhap='sv.22110001'",
                        String.class);
        jdbc.update("UPDATE TAI_KHOAN SET Trang_Thai='Khoa' WHERE Ten_Dang_Nhap='sv.22110001'");
        LocalDateTime deadline =
                jdbc.queryForObject(
                        "SELECT End_SV FROM DOT_DANG_KY WHERE Ma_Dot='MH_2627_HK1'",
                        LocalDateTime.class);
        jdbc.update(
                "INSERT INTO SV (Ma_SV,Ho_Ten,Email) VALUES ('USER_SV','Sinh viên do nhóm"
                        + " nhập','user@example.test')");
        jdbc.update(
                "INSERT INTO NHOM (Ma_Nhom,Ten_Nhom,Ma_Dot) VALUES ('USER_NHOM','Nhóm do người dùng"
                        + " tạo','MH_2627_HK1')");
        jdbc.update(
                "INSERT INTO THANH_VIEN_NHOM (Ma_Nhom,Ma_SV,Vai_Tro) VALUES"
                        + " ('USER_NHOM','USER_SV','Truong_Nhom')");

        importer.run(new DefaultApplicationArguments());

        assertThat(count("TAI_KHOAN")).isEqualTo(28);
        assertThat(count("DE_TAI")).isEqualTo(12);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Mat_Khau_Hash FROM TAI_KHOAN WHERE"
                                        + " Ten_Dang_Nhap='sv.22110001'",
                                String.class))
                .isEqualTo(originalHash);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Trang_Thai FROM TAI_KHOAN WHERE"
                                        + " Ten_Dang_Nhap='sv.22110001'",
                                String.class))
                .isEqualTo("Khoa");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT End_SV FROM DOT_DANG_KY WHERE Ma_Dot='MH_2627_HK1'",
                                LocalDateTime.class))
                .isEqualTo(deadline);
        assertThat(jdbc.queryForObject("SELECT Ho_Ten FROM SV WHERE Ma_SV='USER_SV'", String.class))
                .isEqualTo("Sinh viên do nhóm nhập");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Ten_Nhom FROM NHOM WHERE Ma_Nhom='USER_NHOM'",
                                String.class))
                .isEqualTo("Nhóm do người dùng tạo");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT Ma_SV FROM THANH_VIEN_NHOM WHERE Ma_Nhom='USER_NHOM'",
                                String.class))
                .isEqualTo("USER_SV");
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }
}
