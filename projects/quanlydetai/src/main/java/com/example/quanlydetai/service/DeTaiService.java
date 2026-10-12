package com.example.quanlydetai.service;

import com.example.quanlydetai.dto.DeTaiRequest;
import com.example.quanlydetai.dto.DeTaiResponse;
import com.example.quanlydetai.dto.LuaChonResponse;
import com.example.quanlydetai.entity.BoMon;
import com.example.quanlydetai.entity.DeTai;
import com.example.quanlydetai.entity.DotDangKy;
import com.example.quanlydetai.entity.GiangVien;
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

    public DeTaiService(
            CurrentUserService currentUser,
            JpaEntityRepository entities,
            AcademicRules rules,
            ReferenceDataService referenceData) {
        this.currentUser = currentUser;
        this.entities = entities;
        this.rules = rules;
        this.referenceData = referenceData;
    }

    public List<LuaChonResponse> periods() {
        return referenceData.periods();
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
        var distinct = new LinkedHashSet<>(f.supervisors());
        rules.check(
                distinct.size() == f.supervisors().size(),
                "Giảng viên hướng dẫn không được trùng nhau.");
        distinct.forEach(id -> entities.require(GiangVien.class, id));
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
        for (String id : distinct) {
            GvhdDeTai h = new GvhdDeTai();
            h.setMaGV(id);
            h.setMaDeTai(t.getMaDeTai());
            entities.insert(h);
        }
    }

    @Transactional
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public void review(String id, boolean approve) {
        currentUser.dean();
        DeTai t = entities.lock(DeTai.class, id);
        rules.check("Nhap".equals(t.getTrangThai()), "Chỉ duyệt đề tài đang chờ công bố.");
        rules.check(
                !rules.now().isAfter(entities.require(DotDangKy.class, t.getMaDot()).getEndSV()),
                "Đợt đã kết thúc đăng ký sinh viên.");
        int count =
                entities.query("select h from GvhdDeTai h where h.maDeTai=?1", GvhdDeTai.class, id)
                        .size();
        rules.check(count >= 1 && count <= 2, "Đề tài cần 1–2 giảng viên hướng dẫn.");
        t.setTrangThai(approve ? "DaCongBo" : "Huy");
        entities.flush();
    }

    @PreAuthorize("hasRole('GV')")
    public List<DeTaiResponse> mine() {
        String gv = currentUser.teacher();
        return entities
                .query(
                        "select t from DeTai t where t.maGvDangKy=?1 or t.maDeTai in (select"
                            + " h.maDeTai from GvhdDeTai h where h.maGV=?1) order by t.ngayDangKy"
                            + " desc",
                        DeTai.class,
                        gv)
                .stream()
                .map(this::view)
                .toList();
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public List<DeTaiResponse> all() {
        currentUser.dean();
        return entities
                .query("select t from DeTai t order by t.ngayDangKy desc", DeTai.class)
                .stream()
                .map(this::view)
                .toList();
    }

    @PreAuthorize("hasRole('SV')")
    public List<DeTaiResponse> available() {
        return entities
                .query(
                        "select t from DeTai t where t.trangThai='DaCongBo' order by t.ngayDangKy"
                                + " desc",
                        DeTai.class)
                .stream()
                .map(this::view)
                .toList();
    }

    DeTaiResponse view(DeTai t) {
        var supervisors =
                entities.query(
                        "select g.hoTen from GiangVien g where g.maGV in (select h.maGV from"
                                + " GvhdDeTai h where h.maDeTai=?1)",
                        String.class,
                        t.getMaDeTai());
        var reviewers =
                entities.query(
                        "select g.hoTen from GiangVien g where g.maGV in (select h.maGV from"
                                + " GvpbDeTai h where h.maDeTai=?1)",
                        String.class,
                        t.getMaDeTai());
        var councils =
                entities.query(
                        "select h.maHd from HoiDongDeTai h where h.maDeTai=?1",
                        String.class,
                        t.getMaDeTai());
        var groups =
                entities.query(
                        "select n.maNhom from Nhom n where n.maDeTai=?1",
                        String.class,
                        t.getMaDeTai());
        DotDangKy d = entities.require(DotDangKy.class, t.getMaDot());
        boolean held =
                entities.exists(
                        "select r.id from DangKyDeTai r where r.activeTopic=?1", t.getMaDeTai());
        return new DeTaiResponse(
                t.getMaDeTai(),
                t.getTenDeTai(),
                t.getNoiDung(),
                entities.require(BoMon.class, t.getMaBoMon()).getTenBoMon(),
                d.getTenDotDangKy(),
                t.getTrangThai(),
                supervisors,
                reviewers.isEmpty() ? null : reviewers.getFirst(),
                councils.isEmpty() ? null : councils.getFirst(),
                groups.isEmpty() ? null : groups.getFirst(),
                held,
                rules.inWindow(d.getBeginSV(), d.getEndSV()));
    }
}
