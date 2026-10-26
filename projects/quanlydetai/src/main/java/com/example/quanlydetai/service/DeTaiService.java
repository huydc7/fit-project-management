package com.example.quanlydetai.service;

import com.example.quanlydetai.dto.DeTaiRequest;
import com.example.quanlydetai.dto.DeTaiResponse;
import com.example.quanlydetai.dto.LuaChonResponse;
import com.example.quanlydetai.dto.TopicSearch;
import com.example.quanlydetai.dto.ThanhVienResponse;
import com.example.quanlydetai.entity.BoMon;
import com.example.quanlydetai.entity.DeTai;
import com.example.quanlydetai.entity.DotDangKy;
import com.example.quanlydetai.entity.GvhdDeTai;
import com.example.quanlydetai.repository.DeTaiQueryRepository;
import com.example.quanlydetai.repository.JpaEntityRepository;
import com.example.quanlydetai.security.CurrentUserService;

import org.springframework.data.domain.*;
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

    private final DeTaiQueryRepository queries;

    public DeTaiService(
            CurrentUserService currentUser,
            JpaEntityRepository entities,
            AcademicRules rules,
            ReferenceDataService referenceData,
            GiangVienService teachers,
            DeTaiQueryRepository queries) {
        this.queries = queries;
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

    @PreAuthorize("hasRole('TRUONGBOMON')")
    public List<LuaChonResponse> departmentTeachers() {
        return referenceData.teachersInDepartment(currentUser.departmentHead());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
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
            var supervisors =
                    entities.query(
                            "select h.maGV from GvhdDeTai h where h.maDeTai=?1", String.class, id);
            rules.check(
                    supervisors.size() >= 1 && supervisors.size() <= 2,
                    "Bộ môn phải chọn 1–2 GVHD trước khi công bố.");
            teachers.requireActiveAccounts(supervisors);
            t.setMaGvXacNhan(currentUser.teacher());
            t.setNgayXacNhan(rules.now());
        }
        t.setTrangThai(approve ? "DaCongBo" : "Huy");
        entities.flush();
    }

    @PreAuthorize("hasRole('GV')")
    public List<DeTaiResponse> mine() {
        return mine(TopicSearch.empty(), Paging.defaults()).getContent();
    }

    @PreAuthorize("hasRole('GV')")
    public Page<DeTaiResponse> mine(TopicSearch filter, Pageable page) {
        Paging.check(page);
        var rows =
                queries.search(
                        filter == null ? TopicSearch.empty() : filter,
                        currentUser.teacher(),
                        null,
                        false,
                        page);
        return new PageImpl<>(views(rows.getContent()), page, rows.getTotalElements());
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public List<DeTaiResponse> all() {
        return all(TopicSearch.empty(), Paging.defaults()).getContent();
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public Page<DeTaiResponse> all(TopicSearch filter, Pageable page) {
        Paging.check(page);
        currentUser.dean();
        var rows =
                queries.search(
                        filter == null ? TopicSearch.empty() : filter, null, null, false, page);
        return new PageImpl<>(views(rows.getContent()), page, rows.getTotalElements());
    }

    @PreAuthorize("hasRole('SV')")
    public List<DeTaiResponse> available() {
        return available(TopicSearch.empty(), Paging.defaults()).getContent();
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public Page<DeTaiResponse> pendingApproval(TopicSearch filter, Pageable page) {
        Paging.check(page);
        currentUser.dean();
        var rows = queries.pendingApproval(filter == null ? TopicSearch.empty() : filter, page);
        return new PageImpl<>(views(rows.getContent()), page, rows.getTotalElements());
    }

    @PreAuthorize("hasRole('SV')")
    public Page<DeTaiResponse> available(TopicSearch filter, Pageable page) {
        Paging.check(page);
        var rows =
                queries.search(
                        filter == null ? TopicSearch.empty() : filter, null, null, true, page);
        return new PageImpl<>(views(rows.getContent()), page, rows.getTotalElements());
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
        var groupRows = entities.query(
                "select t.maDeTai,n.maNhom,n.tenNhom,g.hoTen from DeTai t join t.gvDangKy g"
                        + " left join Nhom n on n.maDeTai=t.maDeTai where t.maDeTai in ?1",
                Object[].class, ids);
        var groups = labels(groupRows.stream().filter(row -> row[1] != null).toList());
        var groupNames = new HashMap<String, String>();
        var proposers = new HashMap<String, String>();
        for (var row : groupRows) {
            if (row[1] != null) groupNames.put((String) row[0], (String) row[2]);
            proposers.put((String) row[0], (String) row[3]);
        }
        // Batch membership lookups for this page, never traverse lazy collections per topic.
        Map<String, List<ThanhVienResponse>> groupMembers = groupNames.isEmpty() ? Map.of() : participants(entities.query(
                "select n.maDeTai,s.maSV,s.hoTen,m.vaiTro from ThanhVienNhom m"
                        + " join m.nhom n join m.sinhVien s where n.maDeTai in ?1"
                        + " order by n.maDeTai,case when m.vaiTro='Truong_Nhom' then 0 else 1 end,s.maSV",
                Object[].class, ids));
        Map<String, List<ThanhVienResponse>> councilMembers = councils.isEmpty() ? Map.of() : participants(entities.query(
                "select h.maDeTai,g.maGV,g.hoTen,m.vaiTro from HoiDongDeTai h,"
                        + " ThanhVienHoiDong m join m.giangVien g where h.maHd=m.maHd"
                        + " and h.maDeTai in ?1 order by h.maDeTai,case m.vaiTro"
                        + " when 'ChuTich' then 0 when 'ThuKy' then 1 else 2 end,g.maGV",
                Object[].class, ids));
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
                                    t.getNgayXacNhan(),
                                    proposers.get(id),
                                    groupNames.get(id),
                                    groupMembers.getOrDefault(id, List.of()),
                                    councilMembers.getOrDefault(id, List.of()));
                        })
                .toList();
    }

    private static Map<String, List<ThanhVienResponse>> participants(List<Object[]> rows) {
        var result = new HashMap<String, List<ThanhVienResponse>>();
        for (var row : rows)
            result.computeIfAbsent((String) row[0], ignored -> new ArrayList<>())
                    .add(new ThanhVienResponse((String) row[1], (String) row[2], (String) row[3]));
        return result;
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

    @PreAuthorize("hasRole('GV')")
    public DeTaiResponse editableProposal(String id) {
        DeTai t = entities.require(DeTai.class, id);
        checkEditableTopic(t, entities.require(DotDangKy.class, t.getMaDot()));
        return views(List.of(t)).getFirst();
    }

    private DeTai editableTopic(String id) {
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
        checkEditableTopic(t, d);
        return t;
    }

    private void checkEditableTopic(DeTai t, DotDangKy d) {
        if (!currentUser.teacher().equals(t.getMaGvDangKy()))
            throw new org.springframework.security.access.AccessDeniedException(
                    "Chỉ người đề xuất được sửa/xóa đề tài.");
        rules.check("Nhap".equals(t.getTrangThai()), "Chỉ sửa/xóa đề tài chưa được duyệt.");
        rules.teacherWindow(d);
    }

    @PreAuthorize("hasRole('TRUONGBOMON')")
    public List<DeTaiResponse> departmentTopics() {
        return departmentTopics(TopicSearch.empty(), Paging.defaults()).getContent();
    }

    @PreAuthorize("hasRole('TRUONGBOMON')")
    public Page<DeTaiResponse> departmentTopics(TopicSearch filter, Pageable page) {
        Paging.check(page);
        var rows =
                queries.search(
                        filter == null ? TopicSearch.empty() : filter,
                        null,
                        currentUser.departmentHead(),
                        false,
                        page);
        return new PageImpl<>(views(rows.getContent()), page, rows.getTotalElements());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
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
            teachers.requireDepartmentSupervisors(distinct, department);
            // The period lock serializes approvals. READ_COMMITTED ensures the count
            // sees the previous approval after waiting for that lock on MySQL.
            for (String gv : distinct) {
                long assigned = entities.query(
                        "select count(h) from GvhdDeTai h, DeTai t"
                                + " where h.maDeTai=t.maDeTai and h.maGV=?1 and t.maDot=?2"
                                + " and t.maDeTai<>?3 and t.trangThai<>'Huy'"
                                + " and (t.trangThai<>'Nhap' or exists"
                                + " (select r.maDeTai from DuyetBoMon r where r.maDeTai=t.maDeTai))",
                        Long.class, gv, period, id).getFirst();
                rules.check(assigned < AcademicRules.MAX_SUPERVISED_TOPICS_PER_PERIOD,
                        "Giảng viên " + gv + " đã hướng dẫn đủ 3 đề tài trong đợt này."
                                + " Vui lòng chọn giảng viên khác.");
            }
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
