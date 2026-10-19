package com.example.quanlydetai.tools.sample;

import com.example.quanlydetai.entity.*;
import com.example.quanlydetai.service.TaiKhoanService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/** Explicit local-only import. Existing credentials and academic relationships are preserved. */
@Profile("dev")
@ConditionalOnProperty(name = "app.sample-data.enabled", havingValue = "true")
public class SampleDataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SampleDataInitializer.class);
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    @PersistenceContext private EntityManager entityManager;
    private final PasswordEncoder passwords;
    private final Validator validator;
    private final Path source;
    private final String initialPassword;
    private final boolean refreshExisting;

    public SampleDataInitializer(
            PasswordEncoder passwords,
            Validator validator,
            @Value("${app.sample-data.path}") String source,
            @Value("${app.sample-data.password:}") String initialPassword,
            @Value("${app.sample-data.refresh-existing:false}") boolean refreshExisting) {
        this.passwords = passwords;
        this.validator = validator;
        this.source = Path.of(source);
        this.initialPassword = initialPassword;
        this.refreshExisting = refreshExisting;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) throws Exception {
        if (!refreshExisting && accountCount() > 0) {
            log.info("Sample data skipped: accounts already exist. No existing data was changed.");
            return;
        }
        TaiKhoanService.checkPassword(initialPassword);
        if (!Files.isRegularFile(source) || Files.size(source) > 1_048_576) {
            throw new IllegalStateException(
                    "Sample data file is missing or exceeds 1 MiB. Check SAMPLE_DATA_PATH.");
        }
        FacultySampleData data =
                JsonMapper.builder().build().readValue(source.toFile(), FacultySampleData.class);
        if (!validator.validate(data).isEmpty()) {
            throw new IllegalStateException("Sample data file has invalid required fields.");
        }
        String hash = passwords.encode(initialPassword);
        Map<String, String> teacherIds = new HashMap<>();
        Map<String, String> periodIds = new HashMap<>();

        for (var department : data.departments()) addDepartment(department);
        for (var teacher : data.teachers()) {
            String id = resolveId(GiangVien.class, teacher.id(), teacher.legacyId());
            teacherIds.put(teacher.id(), id);
            GiangVien profile = entityManager.find(GiangVien.class, id);
            boolean isNew = profile == null;
            if (isNew) {
                profile = new GiangVien();
                profile.setMaGV(id);
            }
            profile.setHoTen(teacher.name());
            profile.setEmail(teacher.email());
            profile.setMaBoMon(teacher.department());
            if (isNew) persist(profile);
            account("TK_" + teacher.id(), id, null, teacher.username(), "GV", hash);
            if (teacher.dean() && entityManager.find(TruongKhoa.class, id) == null) {
                TruongKhoa dean = new TruongKhoa();
                dean.setMaGV(id);
                persist(dean);
            }
        }
        for (var student : data.students()) {
            String id = resolveId(SinhVien.class, student.id(), student.legacyId());
            SinhVien profile = entityManager.find(SinhVien.class, id);
            boolean isNew = profile == null;
            if (isNew) {
                profile = new SinhVien();
                profile.setMaSV(id);
            }
            profile.setHoTen(student.name());
            profile.setEmail(student.email());
            if (isNew) persist(profile);
            account("TK_" + student.id(), null, id, student.username(), "SV", hash);
        }
        var administrator = data.administrator();
        String adminId = resolveId(TaiKhoan.class, administrator.id(), administrator.legacyId());
        TaiKhoan admin = entityManager.find(TaiKhoan.class, adminId);
        if (admin == null) {
            admin = newAccount(adminId, administrator.username(), "Admin", hash);
            persist(admin);
        } else admin.setTenDangNhap(administrator.username());

        String deanId =
                data.teachers().stream()
                        .filter(FacultySampleData.Teacher::dean)
                        .map(t -> teacherIds.get(t.id()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Sample dataset must contain a dean."));
        LocalDateTime today = LocalDate.now(VIETNAM).atTime(8, 0);
        for (var period : data.periods()) {
            String id = resolveId(DotDangKy.class, period.id(), period.legacyId());
            periodIds.put(period.id(), id);
            DotDangKy existing = entityManager.find(DotDangKy.class, id);
            if (existing != null) {
                existing.setTenDotDangKy(period.name());
                continue;
            }
            validatePeriod(period);
            DotDangKy entity = new DotDangKy();
            entity.setMaDot(id);
            entity.setMaTruongKhoa(deanId);
            entity.setTenDotDangKy(period.name());
            entity.setLoai(period.type());
            entity.setBeginGV(today.plusDays(period.teacherStartDays()));
            entity.setEndGV(today.plusDays(period.teacherEndDays()));
            entity.setBeginSV(today.plusDays(period.studentStartDays()));
            entity.setEndSV(today.plusDays(period.studentEndDays()));
            if (period.reviewDeadlineDays() != null)
                entity.setDeadlineGvpbNopDiem(today.plusDays(period.reviewDeadlineDays()));
            if (period.councilDays() != null)
                entity.setTimeBcHd(today.plusDays(period.councilDays()));
            persist(entity);
        }
        for (var topic : data.topics()) {
            String id = resolveId(DeTai.class, topic.id(), topic.legacyId());
            DeTai existing = entityManager.find(DeTai.class, id);
            if (existing != null) {
                existing.setTenDeTai(topic.title());
                existing.setNoiDung(topic.content());
                existing.setMaBoMon(topic.department());
                continue;
            }
            DotDangKy period = entityManager.find(DotDangKy.class, periodIds.get(topic.period()));
            if (period == null
                    || topic.supervisors().stream().distinct().count()
                            != topic.supervisors().size()) {
                throw new IllegalStateException(
                        "Sample topic has an unknown period or duplicate supervisors.");
            }
            DeTai entity = new DeTai();
            entity.setMaDeTai(id);
            entity.setTenDeTai(topic.title());
            entity.setNoiDung(topic.content());
            entity.setMaBoMon(topic.department());
            entity.setMaDot(period.getMaDot());
            entity.setMaGvDangKy(requiredMapping(teacherIds, topic.supervisors().getFirst()));
            entity.setNgayDangKy(period.getBeginGV().plusHours(1));
            entity.setTrangThai("Nhap");
            persist(entity);
            for (String supervisor : topic.supervisors()) {
                GvhdDeTai association = new GvhdDeTai();
                association.setMaDeTai(id);
                association.setMaGV(requiredMapping(teacherIds, supervisor));
                persist(association);
            }
            // Strict phase gate also applies to new sample topics; never reopen an existing period.
            if (topic.published() && LocalDateTime.now(VIETNAM).isBefore(period.getBeginSV()))
                entity.setTrangThai("DaCongBo");
            entityManager.flush();
        }
        var notice = data.notice();
        String noticeId = resolveId(ThongBao.class, notice.id(), notice.legacyId());
        ThongBao existingNotice = entityManager.find(ThongBao.class, noticeId);
        boolean isNewNotice = existingNotice == null;
        if (isNewNotice) {
            existingNotice = new ThongBao();
            existingNotice.setMaTb(noticeId);
            existingNotice.setMaTk(adminId);
            existingNotice.setPhamVi("NhaTruong");
            existingNotice.setNgayDang(LocalDateTime.now(VIETNAM));
        }
        existingNotice.setTieuDe(notice.title());
        existingNotice.setNoiDung(notice.content());
        if (isNewNotice) persist(existingNotice);
        entityManager.flush();
        log.info(
                "Faculty sample data imported. Existing passwords, account status, group"
                        + " memberships, reports, scores and period dates were preserved.");
    }

    private long accountCount() {
        return entityManager
                .createQuery("select count(a) from TaiKhoan a", Long.class)
                .getSingleResult();
    }

    private <T> String resolveId(Class<T> type, String id, String legacyId) {
        if (entityManager.find(type, id) != null) return id;
        if (refreshExisting && legacyId != null && entityManager.find(type, legacyId) != null)
            return legacyId;
        return id;
    }

    private void persist(Object entity) {
        entityManager.persist(entity);
        entityManager.flush();
        entityManager.refresh(entity);
    }

    private void addDepartment(FacultySampleData.Department source) {
        if (entityManager.find(BoMon.class, source.id()) == null) {
            BoMon department = new BoMon();
            department.setMaBoMon(source.id());
            department.setTenBoMon(source.name());
            persist(department);
        }
    }

    private void account(
            String id, String teacher, String student, String username, String role, String hash) {
        var matches =
                entityManager
                        .createQuery(
                                "select a from TaiKhoan a where a.maGV = :teacher or a.maSV ="
                                        + " :student",
                                TaiKhoan.class)
                        .setParameter("teacher", teacher)
                        .setParameter("student", student)
                        .getResultList();
        if (!matches.isEmpty()) {
            matches.getFirst().setTenDangNhap(username);
            return;
        }
        TaiKhoan account = newAccount(id, username, role, hash);
        account.setMaGV(teacher);
        account.setMaSV(student);
        persist(account);
    }

    private TaiKhoan newAccount(String id, String username, String role, String hash) {
        TaiKhoan account = new TaiKhoan();
        account.setMaTk(id);
        account.setTenDangNhap(username);
        account.setVaiTro(role);
        account.setMatKhauHash(hash);
        account.setTrangThai("HoatDong");
        account.setNgayTao(LocalDateTime.now(VIETNAM));
        return account;
    }

    private String requiredMapping(Map<String, String> mapping, String id) {
        String resolved = mapping.get(id);
        if (resolved == null)
            throw new IllegalStateException("Sample data references an unknown identifier.");
        return resolved;
    }

    private void validatePeriod(FacultySampleData.Period period) {
        if (period.teacherStartDays() >= period.teacherEndDays()
                || period.teacherEndDays() >= period.studentStartDays()
                || period.studentStartDays() >= period.studentEndDays()
                || (("TLCN".equals(period.type()) || "KLTN".equals(period.type()))
                        && period.reviewDeadlineDays() == null)
                || ("KLTN".equals(period.type()) && period.councilDays() == null)) {
            throw new IllegalStateException(
                    "Sample period phases or required deadlines are invalid.");
        }
    }
}
