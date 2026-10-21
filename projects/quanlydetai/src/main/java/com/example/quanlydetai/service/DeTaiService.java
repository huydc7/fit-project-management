package com.example.quanlydetai.service;

import com.example.quanlydetai.dto.DeTaiRequest;
import com.example.quanlydetai.dto.DeTaiResponse;
import com.example.quanlydetai.dto.LuaChonResponse;
import com.example.quanlydetai.entity.BoMon;
import com.example.quanlydetai.entity.DeTai;
import com.example.quanlydetai.entity.DotDangKy;
import com.example.quanlydetai.entity.GvhdDeTai;
import com.example.quanlydetai.repository.JpaEntityRepository;
import com.example.quanlydetai.security.CurrentUserService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;

@Service
@Validated
@Transactional(readOnly = true)
public class DeTaiService {
    private final CurrentUserService currentUser;
    private final JpaEntityRepository entities;
    private final AcademicRules rules;
    private final ReferenceDataService referenceData;
    private final GiangVienService teachers;

    public DeTaiService(
            CurrentUserService currentUser,
            JpaEntityRepository entities,
            AcademicRules rules,
            ReferenceDataService referenceData,
            GiangVienService teachers) {
        this.currentUser = currentUser;
        this.entities = entities;
        this.rules = rules;
        this.referenceData = referenceData;
        this.teachers = teachers;
    }

    public List<LuaChonResponse> periods() {
        var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return entities
                .query("select d from DotDangKy d order by d.beginGV desc", DotDangKy.class)
                .stream()
                .map(
                        d ->
                                new LuaChonResponse(
                                        d.getMaDot(),
                                        d.getTenDotDangKy()
                                                + " · "
                                                + d.getBeginGV().format(fmt)
                                                + " → "
                                                + d.getEndGV().format(fmt)
                                                + (rules.inWindow(d.getBeginGV(), d.getEndGV())
                                                        ? " · Đang nhận đề xuất"
                                                        : " · Ngoài thời gian đề xuất")))
                .toList();
    }

    public List<LuaChonResponse> departments() {
        return referenceData.departments();
    }

    public List<LuaChonResponse> teachers() {
        return referenceData.teachers();
    }

    @Transactional
    @PreAuthorize("hasRole('GV')")
    public void create(@jakarta.validation.Valid DeTaiRequest f) {
        String gv = currentUser.teacher();
        DotDangKy dot = entities.lock(DotDangKy.class, f.period());
        rules.teacherWindow(dot);
        entities.require(BoMon.class, f.department());
        DeTai t = new DeTai();
        t.setMaDeTai(rules.id("DT"));
        t.setTenDeTai(f.title().strip());
        t.setNoiDung(f.content().strip());
        t.setMaBoMon(f.department());
        t.setMaDot(dot.getMaDot());
        t.setMaGvDangKy(gv);
        t.setTrangThai("Nhap");
        t.setNgayDangKy(rules.now());
        entities.insert(t);
    }

    @Transactional
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public void review(String id, boolean approve) {
        currentUser.dean();
        String period =
                entities
                        .query("select t.maDot from DeTai t where t.maDeTai=?1", String.class, id)
                        .stream()
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new com.example.quanlydetai.exception.BusinessException(
                                                "Không tìm thấy đề tài."));
        DotDangKy dot = entities.lock(DotDangKy.class, period);
        DeTai t = entities.lock(DeTai.class, id);
        if (approve)
            rules.check(
                    entities.find(com.example.quanlydetai.entity.DuyetBoMon.class, id) != null,
                    "Đề tài cần được Trưởng bộ môn duyệt trước khi công bố.");
        rules.check("Nhap".equals(t.getTrangThai()), "Chỉ duyệt đề tài đang chờ công bố.");
        rules.topicReviewWindow(dot);
        if (approve) {
            int count =
                    entities.query(
                                    "select h from GvhdDeTai h where h.maDeTai=?1",
                                    GvhdDeTai.class,
                                    id)
                            .size();
            rules.check(count >= 1 && count <= 2, "Bộ môn phải chọn 1–2 GVHD trước khi công bố.");
            teachers.requireActiveAccounts(
                    entities.query(
                            "select h.maGV from GvhdDeTai h where h.maDeTai=?1", String.class, id));
            t.setMaGvXacNhan(currentUser.teacher());
            t.setNgayXacNhan(rules.now());
        }
        t.setTrangThai(approve ? "DaCongBo" : "Huy");
        entities.flush();
    }

    @PreAuthorize("hasRole('GV')")
    public List<DeTaiResponse> mine() {
        String gv = currentUser.teacher();
        return views(
                entities.query(
                        "select t from DeTai t join fetch t.dot join fetch t.boMon where"
                            + " t.maGvDangKy=?1 or t.maDeTai in (select h.maDeTai from GvhdDeTai h"
                            + " where h.maGV=?1) order by t.ngayDangKy desc, t.maDeTai",
                        DeTai.class,
                        gv));
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public List<DeTaiResponse> all() {
        currentUser.dean();
        return views(
                entities.query(
                        "select t from DeTai t join fetch t.dot join fetch t.boMon order by"
                                + " t.ngayDangKy desc, t.maDeTai",
                        DeTai.class));
    }

    @PreAuthorize("hasRole('SV')")
    public List<DeTaiResponse> available() {
        return views(
                entities.query(
                        "select t from DeTai t join fetch t.dot join fetch t.boMon where"
                                + " t.trangThai='DaCongBo' order by t.ngayDangKy desc, t.maDeTai",
                        DeTai.class));
    }

    // Load related lists once per result set, rather than issuing queries for each topic.
    private List<DeTaiResponse> views(List<DeTai> topics) {
        if (topics.isEmpty()) return List.of();
        var ids = topics.stream().map(DeTai::getMaDeTai).toList();
        var supervisorRows =
                entities.query(
                        "select h.maDeTai, g.hoTen, g.maGV from GvhdDeTai h join h.giangVien g"
                                + " where h.maDeTai in ?1 order by g.maGV",
                        Object[].class,
                        ids);
        var supervisors = labels(supervisorRows);
        var supervisorIds = new HashMap<String, List<String>>();
        for (var row : supervisorRows)
            supervisorIds
                    .computeIfAbsent((String) row[0], ignored -> new ArrayList<>())
                    .add((String) row[2]);
        var reviewers =
                labels(
                        entities.query(
                                "select h.maDeTai, g.hoTen from GvpbDeTai h join h.giangVien g"
                                        + " where h.maDeTai in ?1 order by g.maGV",
                                Object[].class,
                                ids));
        var councils =
                labels(
                        entities.query(
                                "select h.maDeTai, h.maHd from HoiDongDeTai h where h.maDeTai in"
                                        + " ?1",
                                Object[].class,
                                ids));
        var groups =
                labels(
                        entities.query(
                                "select n.maDeTai, n.maNhom from Nhom n where n.maDeTai in ?1",
                                Object[].class,
                                ids));
        var held =
                new HashSet<>(
                        entities.query(
                                "select r.activeTopic from DangKyDeTai r where r.activeTopic in ?1",
                                String.class,
                                ids));
        var approved =
                new HashSet<>(
                        entities.query(
                                "select r.maDeTai from DuyetBoMon r where r.maDeTai in ?1",
                                String.class,
                                ids));
        String teacher = currentUser.actor().getMaGV();
        return topics.stream()
                .map(
                        t -> {
                            DotDangKy d = t.getDot();
                            String id = t.getMaDeTai();
                            return new DeTaiResponse(
                                    id,
                                    t.getTenDeTai(),
                                    t.getNoiDung(),
                                    t.getBoMon().getTenBoMon(),
                                    d.getTenDotDangKy(),
                                    t.getTrangThai(),
                                    supervisors.getOrDefault(id, List.of()),
                                    first(reviewers, id),
                                    first(councils, id),
                                    first(groups, id),
                                    held.contains(id),
                                    rules.inWindow(d.getBeginSV(), d.getEndSV()),
                                    t.getMaDot(),
                                    t.getMaBoMon(),
                                    "Nhap".equals(t.getTrangThai())
                                            && Objects.equals(t.getMaGvDangKy(), teacher)
                                            && rules.inWindow(d.getBeginGV(), d.getEndGV()),
                                    approved.contains(id),
                                    supervisorIds.getOrDefault(id, List.of()),
                                    d.getLoai(),
                                    rules.now().isBefore(d.getBeginSV()),
                                    t.getMaGvDangKy(),
                                    t.getMaGvXacNhan(),
                                    t.getNgayXacNhan());
                        })
                .toList();
    }

    private static Map<String, List<String>> labels(List<Object[]> rows) {
        var result = new HashMap<String, List<String>>();
        for (var row : rows)
            result.computeIfAbsent((String) row[0], ignored -> new ArrayList<>())
                    .add((String) row[1]);
        return result;
    }

    private static String first(Map<String, List<String>> values, String id) {
        var rows = values.get(id);
        return rows == null || rows.isEmpty() ? null : rows.getFirst();
    }

    public static List<DeTaiResponse> filter(
            List<DeTaiResponse> rows, String q, String department, String period) {
        return filter(rows, q, department, period, "", "");
    }

    public static List<DeTaiResponse> filter(
            List<DeTaiResponse> rows,
            String q,
            String department,
            String period,
            String type,
            String state) {
        return rows.stream()
                .filter(t -> SearchText.matches(q, t.id(), t.title(), t.content()))
                .filter(
                        t ->
                                department == null
                                        || department.isBlank()
                                        || department.equals(t.departmentId()))
                .filter(t -> period == null || period.isBlank() || period.equals(t.periodId()))
                .filter(t -> type == null || type.isBlank() || type.equals(t.periodType()))
                .filter(t -> state == null || state.isBlank() || state.equals(t.state()))
                .toList();
    }

    @Transactional
    @PreAuthorize("hasRole('GV')")
    public void updateContent(
            String id,
            @jakarta.validation.Valid com.example.quanlydetai.dto.NoiDungDeTaiRequest f) {
        DeTai t = editableTopic(id);
        if (f.department() != null) {
            entities.require(BoMon.class, f.department());
        }
        // Invalidate approval before changing assignments; MySQL locks an approved list.
        var approval = entities.find(com.example.quanlydetai.entity.DuyetBoMon.class, id);
        if (approval != null) entities.remove(approval);
        entities.flush();
        entities.query("select h from GvhdDeTai h where h.maDeTai=?1", GvhdDeTai.class, id)
                .forEach(entities::remove);
        entities.flush();
        if (f.department() != null) t.setMaBoMon(f.department());
        t.setTenDeTai(f.title().strip());
        t.setNoiDung(f.content().strip());
        entities.flush();
    }

    @Transactional
    @PreAuthorize("hasRole('GV')")
    public void deleteDraft(String id) {
        DeTai t = editableTopic(id);
        rules.check(
                !entities.exists("select r from DangKyDeTai r where r.maDeTai=?1", id),
                "Đề tài đã có lịch sử đăng ký; không được xóa.");
        var approval = entities.find(com.example.quanlydetai.entity.DuyetBoMon.class, id);
        if (approval != null) entities.remove(approval);
        entities.flush();
        entities.query("select h from GvhdDeTai h where h.maDeTai=?1", GvhdDeTai.class, id)
                .forEach(entities::remove);
        entities.flush();
        entities.remove(t);
        entities.flush();
    }

    private DeTai editableTopic(String id) {
        String gv = currentUser.teacher();
        String period =
                entities
                        .query("select t.maDot from DeTai t where t.maDeTai=?1", String.class, id)
                        .stream()
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new com.example.quanlydetai.exception.BusinessException(
                                                "Không tìm thấy đề tài."));
        DotDangKy d = entities.lock(DotDangKy.class, period);
        DeTai t = entities.lock(DeTai.class, id);
        if (!gv.equals(t.getMaGvDangKy()))
            throw new org.springframework.security.access.AccessDeniedException(
                    "Chỉ người đề xuất được sửa/xóa đề tài.");
        rules.check("Nhap".equals(t.getTrangThai()), "Chỉ sửa/xóa đề tài chưa được duyệt.");
        rules.teacherWindow(d);
        return t;
    }

    @PreAuthorize("hasRole('TRUONGBOMON')")
    public List<DeTaiResponse> departmentTopics() {
        String department = currentUser.departmentHead();
        return views(
                entities.query(
                        "select t from DeTai t join fetch t.dot join fetch t.boMon where"
                                + " t.maBoMon=?1 order by t.ngayDangKy desc, t.maDeTai",
                        DeTai.class,
                        department));
    }

    @Transactional
    @PreAuthorize("hasRole('TRUONGBOMON')")
    public void reviewDepartment(String id, boolean approve, List<String> supervisors) {
        String department = currentUser.departmentHead();
        String period =
                entities
                        .query("select t.maDot from DeTai t where t.maDeTai=?1", String.class, id)
                        .stream()
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new com.example.quanlydetai.exception.BusinessException(
                                                "Không tìm thấy đề tài."));
        DotDangKy d = entities.lock(DotDangKy.class, period);
        DeTai t = entities.lock(DeTai.class, id);
        if (!department.equals(t.getMaBoMon()))
            throw new org.springframework.security.access.AccessDeniedException(
                    "Chỉ duyệt đề tài thuộc bộ môn phụ trách.");
        rules.check("Nhap".equals(t.getTrangThai()), "Chỉ duyệt đề tài đang chờ công bố.");
        rules.topicReviewWindow(d);
        var review = entities.find(com.example.quanlydetai.entity.DuyetBoMon.class, id);
        if (approve) {
            rules.check(review == null, "Đề tài đã được bộ môn duyệt.");
            rules.check(
                    supervisors != null && supervisors.size() >= 1 && supervisors.size() <= 2,
                    "Chọn từ 1 đến 2 giảng viên hướng dẫn.");
            var distinct = new LinkedHashSet<>(supervisors);
            rules.check(
                    distinct.size() == supervisors.size(),
                    "Giảng viên hướng dẫn không được trùng nhau.");
            for (String gv : distinct) {
                rules.check(
                        gv != null && !gv.isBlank() && gv.length() <= 20, "Mã GVHD không hợp lệ.");
            }
            teachers.requireActiveAccounts(distinct);
            entities.query("select h from GvhdDeTai h where h.maDeTai=?1", GvhdDeTai.class, id)
                    .forEach(entities::remove);
            entities.flush();
            for (String gv : distinct) {
                var assignment = new GvhdDeTai();
                assignment.setMaDeTai(id);
                assignment.setMaGV(gv);
                entities.insert(assignment);
            }
            entities.flush();
            review = new com.example.quanlydetai.entity.DuyetBoMon();
            review.setMaDeTai(id);
            review.setMaGV(currentUser.teacher());
            review.setNgayDuyet(rules.now());
            entities.insert(review);
            entities.flush();
        } else {
            if (review != null) entities.remove(review);
            t.setTrangThai("Huy");
            entities.flush();
        }
    }
}
