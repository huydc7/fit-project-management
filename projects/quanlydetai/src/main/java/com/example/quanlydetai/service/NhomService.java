package com.example.quanlydetai.service;

import com.example.quanlydetai.dto.CapNhatNhomRequest;
import com.example.quanlydetai.dto.DangKyDeTaiResponse;
import com.example.quanlydetai.dto.LoiMoiResponse;
import com.example.quanlydetai.dto.LuaChonResponse;
import com.example.quanlydetai.dto.NhomOption;
import com.example.quanlydetai.dto.NhomRequest;
import com.example.quanlydetai.dto.NhomResponse;
import com.example.quanlydetai.dto.ThanhVienResponse;
import com.example.quanlydetai.entity.DangKyDeTai;
import com.example.quanlydetai.entity.DeTai;
import com.example.quanlydetai.entity.DotDangKy;
import com.example.quanlydetai.entity.LoiMoiNhom;
import com.example.quanlydetai.entity.Nhom;
import com.example.quanlydetai.entity.SinhVien;
import com.example.quanlydetai.entity.ThanhVienNhom;
import com.example.quanlydetai.entity.ThanhVienNhomId;
import com.example.quanlydetai.repository.JpaEntityRepository;
import com.example.quanlydetai.repository.NhomQueryRepository;
import com.example.quanlydetai.security.CurrentUserService;

import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;

@Service
@Validated
@Transactional(readOnly = true)
public class NhomService {
    private final CurrentUserService currentUser;
    private final JpaEntityRepository entities;
    private final AcademicRules rules;
    private final ReferenceDataService referenceData;
    private final NhomQueryRepository queries;

    public NhomService(
            CurrentUserService currentUser,
            JpaEntityRepository entities,
            AcademicRules rules,
            ReferenceDataService referenceData,
            NhomQueryRepository queries) {
        this.queries = queries;
        this.currentUser = currentUser;
        this.entities = entities;
        this.rules = rules;
        this.referenceData = referenceData;
    }

    public List<LuaChonResponse> periods() {
        return referenceData.periods();
    }

    @Transactional
    @PreAuthorize("hasRole('SV')")
    public void create(@jakarta.validation.Valid NhomRequest f) {
        String sv = currentUser.student();
        DotDangKy d = entities.lock(DotDangKy.class, f.period());
        rules.studentWindow(d);
        entities.lock(SinhVien.class, sv);
        noOtherGroup(sv, d.getMaDot());
        Nhom n = new Nhom();
        n.setMaNhom(rules.id("N"));
        n.setTenNhom(f.name().strip());
        n.setMaDot(d.getMaDot());
        entities.insert(n);
        addMember(n.getMaNhom(), sv, "Truong_Nhom");
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('SV')")
    public void rename(String group, @jakarta.validation.Valid CapNhatNhomRequest f) {
        Nhom n = lockPeriodGroup(group);
        requireLeader(n);
        rules.check(isEditable(n), "Không được sửa tên nhóm khi đăng ký đang chờ duyệt hoặc đã được chấp thuận.");
        rules.studentWindow(entities.require(DotDangKy.class, n.getMaDot()));
        n.setTenNhom(f.name().strip());
        entities.flush();
    }

    @Transactional
    @PreAuthorize("hasRole('SV')")
    public void invite(String group, String student) {
        Nhom n = lockPeriodGroup(group);
        requireLeader(n);
        editable(n);
        rules.studentWindow(entities.require(DotDangKy.class, n.getMaDot()));
        entities.require(SinhVien.class, student);
        noOtherGroup(student, n.getMaDot());
        rules.check(
                !entities.exists(
                        "select t.maSV from ThanhVienNhom t where t.maNhom=?1 and t.maSV=?2",
                        group,
                        student),
                "Sinh viên đã ở trong nhóm.");
        rules.groupHasSpace(members(group).size());
        var previous =
                entities.query(
                        "select m from LoiMoiNhom m where m.maNhom=?1 and m.maSV=?2",
                        LoiMoiNhom.class,
                        group,
                        student);
        if (!previous.isEmpty()) {
            LoiMoiNhom old = previous.getFirst();
            rules.check(!"Cho".equals(old.getTrangThai()), "Lời mời đang chờ phản hồi.");
            old.setTrangThai("Cho");
            old.setNgayMoi(rules.now());
            entities.flush();
            return;
        }
        LoiMoiNhom m = new LoiMoiNhom();
        m.setId(rules.id("LM"));
        m.setMaNhom(group);
        m.setMaSV(student);
        m.setTrangThai("Cho");
        m.setNgayMoi(rules.now());
        entities.insert(m);
    }

    @Transactional
    @PreAuthorize("hasRole('SV')")
    public void answer(String invitation, boolean accept) {
        String sv = currentUser.student();
        LoiMoiNhom initial = entities.require(LoiMoiNhom.class, invitation);
        if (!sv.equals(initial.getMaSV()))
            throw new AccessDeniedException("Lời mời không thuộc về bạn.");
        Nhom n = lockPeriodGroup(initial.getMaNhom());
        entities.lock(SinhVien.class, sv);
        LoiMoiNhom m = entities.lock(LoiMoiNhom.class, invitation);
        rules.check("Cho".equals(m.getTrangThai()), "Lời mời đã được xử lý.");
        if (accept) {
            editable(n);
            rules.studentWindow(entities.require(DotDangKy.class, n.getMaDot()));
            noOtherGroup(sv, n.getMaDot());
            rules.groupHasSpace(members(n.getMaNhom()).size());
            addMember(n.getMaNhom(), sv, "Thanh_Vien");
        }
        m.setTrangThai(accept ? "XacNhan" : "TuChoi");
        entities.flush();
    }

    @Transactional
    @PreAuthorize("hasRole('SV')")
    public void removeMember(String group, String student) {
        Nhom n = lockPeriodGroup(group);
        editable(n);
        rules.studentWindow(entities.require(DotDangKy.class, n.getMaDot()));
        String sv = currentUser.student();
        rules.check(
                sv.equals(student) || isLeader(n, sv),
                "Bạn không có quyền thay đổi thành viên này.");
        ThanhVienNhom member =
                entities.require(ThanhVienNhom.class, new ThanhVienNhomId(group, student));
        rules.check(!"Truong_Nhom".equals(member.getVaiTro()), "Nhóm trưởng phải ở lại nhóm.");
        entities.remove(member);
        entities.flush();
    }

    @Transactional
    @PreAuthorize("hasRole('SV')")
    public void register(String group, String topic) {
        Nhom n = lockPeriodGroup(group);
        requireLeader(n);
        editable(n);
        DotDangKy d = entities.require(DotDangKy.class, n.getMaDot());
        rules.studentWindow(d);
        DeTai t = entities.lock(DeTai.class, topic);
        rules.check(t.getMaDot().equals(n.getMaDot()), "Nhóm và đề tài phải cùng đợt.");
        rules.check(
                "DaCongBo".equals(t.getTrangThai()), "Đề tài chưa được công bố hoặc đã có nhóm.");
        rules.check(
                !entities.exists("select r.id from DangKyDeTai r where r.activeTopic=?1", topic),
                "Đề tài đã có nhóm đăng ký chờ duyệt.");
        var members = members(group);
        rules.registrationGroupSize(members.size());
        rules.check(
                members.stream().filter(m -> "Truong_Nhom".equals(m.getVaiTro())).count() == 1,
                "Nhóm phải có đúng một nhóm trưởng.");
        DangKyDeTai r = new DangKyDeTai();
        r.setId(rules.id("DK"));
        r.setMaNhom(group);
        r.setMaDeTai(topic);
        r.setTrangThai("ChoDuyet");
        r.setNgayDangKy(rules.now());
        r.setActiveGroup(group);
        r.setActiveTopic(topic);
        entities.insert(r);
    }

    @Transactional
    @PreAuthorize("hasRole('SV')")
    public void withdraw(String registration) {
        DangKyDeTai initial = entities.require(DangKyDeTai.class, registration);
        Nhom n = lockPeriodGroup(initial.getMaNhom());
        requireLeader(n);
        rules.studentWindow(entities.require(DotDangKy.class, n.getMaDot()));
        DangKyDeTai r = entities.lock(DangKyDeTai.class, registration);
        rules.check("ChoDuyet".equals(r.getTrangThai()), "Chỉ rút đăng ký đang chờ duyệt.");
        r.setTrangThai("Rut");
        r.setActiveGroup(null);
        r.setActiveTopic(null);
        r.setNgayDuyet(rules.now());
        entities.flush();
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public void review(String registration, boolean accept, String reason) {
        String dean = currentUser.dean();
        // Read only IDs before waiting: do not cache a stale pending decision or group.
        var initial = entities.query(
                "select r.maNhom,r.maDeTai,n.maDot from DangKyDeTai r, Nhom n"
                        + " where r.maNhom=n.maNhom and r.id=?1", Object[].class, registration);
        rules.check(!initial.isEmpty(), "Không tìm thấy dữ liệu yêu cầu.");
        Object[] ids = initial.getFirst();
        entities.lock(DotDangKy.class, (String) ids[2]);
        DeTai t = entities.lock(DeTai.class, (String) ids[1]);
        Nhom n = entities.lock(Nhom.class, (String) ids[0]);
        DangKyDeTai r = entities.lock(DangKyDeTai.class, registration);
        rules.check("ChoDuyet".equals(r.getTrangThai()), "Đăng ký đã được xử lý.");
        rules.check(
                reason != null && reason.length() <= 1000 && (accept || !reason.isBlank()),
                "Từ chối phải ghi lý do (tối đa 1.000 ký tự).");
        if (accept) {
            rules.registrationGroupSize(members(n.getMaNhom()).size());
            rules.check(
                    n.getMaDeTai() == null && "DaCongBo".equals(t.getTrangThai()),
                    "Nhóm hoặc đề tài không còn khả dụng.");
            n.setMaDeTai(t.getMaDeTai());
            n.setNgayChonDeTai(rules.now());
            entities.flush();
            t.setTrangThai("DangThucHien");
            entities.flush();
            r.setTrangThai("ChapThuan");
        } else {
            r.setTrangThai("TuChoi");
            r.setActiveGroup(null);
            r.setActiveTopic(null);
        }
        r.setMaGvDuyet(dean);
        r.setLyDo(reason.strip());
        r.setNgayDuyet(rules.now());
        entities.flush();
    }

    @PreAuthorize("hasRole('SV')")
    public List<NhomOption> registrationOptions(List<String> periods) {
        return queries.registrationOptions(currentUser.student(), periods);
    }

    @PreAuthorize("hasRole('SV')")
    public List<NhomOption> reportOptions() {
        return queries.reportOptions(currentUser.student());
    }

    @PreAuthorize("hasRole('SV')")
    public List<NhomResponse> mine() {
        return mine(Paging.defaults()).getContent();
    }

    @PreAuthorize("hasRole('SV')")
    public Page<NhomResponse> mine(Pageable page) {
        Paging.check(page);
        String student = currentUser.student();
        var rows = queries.mine(student, page);
        var ids = rows.stream().map(row -> ((Nhom) row[0]).getMaNhom()).toList();
        var members = new HashMap<String, List<ThanhVienResponse>>();
        var registrations = new HashMap<String, List<DangKyDeTaiResponse>>();
        var reserved = new HashSet<String>();
        if (!ids.isEmpty()) {
            for (var row : queries.members(ids))
                members.computeIfAbsent((String) row[0], ignored -> new ArrayList<>())
                        .add(
                                new ThanhVienResponse(
                                        (String) row[1], (String) row[2], (String) row[3]));
            for (var row : queries.registrations(ids))
                registrations.computeIfAbsent(row.group(), ignored -> new ArrayList<>()).add(row);
            reserved.addAll(queries.activeGroups(ids));
        }
        return rows.map(
                row -> {
                    Nhom n = (Nhom) row[0];
                    var team = members.getOrDefault(n.getMaNhom(), List.of());
                    boolean leader =
                            team.stream()
                                    .anyMatch(
                                            m ->
                                                    student.equals(m.id())
                                                            && "Truong_Nhom".equals(m.role()));
                    return new NhomResponse(
                            n.getMaNhom(),
                            n.getTenNhom(),
                            (String) row[1],
                            n.getMaDeTai(),
                            (String) row[2],
                            leader,
                            n.getMaDeTai() == null && !reserved.contains(n.getMaNhom())
                                    && rules.inWindow((java.time.LocalDateTime) row[3], (java.time.LocalDateTime) row[4]),
                            team,
                            registrations.getOrDefault(n.getMaNhom(), List.of()));
                });
    }

    @PreAuthorize("hasRole('SV')")
    public List<LoiMoiResponse> invitations() {
        return invitations(Paging.defaults()).getContent();
    }

    @PreAuthorize("hasRole('SV')")
    public Page<LoiMoiResponse> invitations(Pageable page) {
        Paging.check(page);
        return queries.invitations(currentUser.student(), page);
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public List<DangKyDeTaiResponse> pending() {
        return pending(Paging.defaults()).getContent();
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public Page<DangKyDeTaiResponse> pending(Pageable page) {
        Paging.check(page);
        currentUser.dean();
        return queries.pending(page);
    }

    private Nhom lockPeriodGroup(String id) {
        var periods = entities.query("select n.maDot from Nhom n where n.maNhom=?1", String.class, id);
        rules.check(!periods.isEmpty(), "Không tìm thấy dữ liệu yêu cầu.");
        entities.lock(DotDangKy.class, periods.getFirst());
        return entities.lock(Nhom.class, id);
    }

    private List<ThanhVienNhom> members(String group) {
        return entities.query(
                "select m from ThanhVienNhom m where m.maNhom=?1", ThanhVienNhom.class, group);
    }

    private boolean isLeader(Nhom n, String sv) {
        var m = entities.find(ThanhVienNhom.class, new ThanhVienNhomId(n.getMaNhom(), sv));
        return m != null && "Truong_Nhom".equals(m.getVaiTro());
    }

    private void requireLeader(Nhom n) {
        if (!isLeader(n, currentUser.student()))
            throw new AccessDeniedException("Chỉ nhóm trưởng được thực hiện.");
    }

    private boolean isEditable(Nhom n) {
        return n.getMaDeTai() == null
                && !entities.exists(
                        "select r.id from DangKyDeTai r where r.activeGroup=?1", n.getMaNhom());
    }

    private void editable(Nhom n) {
        rules.check(
                isEditable(n),
                "Thành viên và đề tài bị khóa khi đăng ký đang chờ duyệt hoặc đã được chấp thuận.");
    }

    private void noOtherGroup(String sv, String period) {
        rules.check(
                !entities.exists(
                        "select m.maSV from ThanhVienNhom m where m.maSV=?1 and m.maNhom in (select"
                                + " n.maNhom from Nhom n where n.maDot=?2)",
                        sv,
                        period),
                "Sinh viên đã có nhóm trong đợt này.");
    }

    private void addMember(String group, String sv, String role) {
        ThanhVienNhom m = new ThanhVienNhom();
        m.setMaNhom(group);
        m.setMaSV(sv);
        m.setVaiTro(role);
        entities.insert(m);
    }
}
