package com.example.quanlydetai.service;

import com.example.quanlydetai.dto.DiemRequest;
import com.example.quanlydetai.dto.KetQuaResponse;
import com.example.quanlydetai.dto.PhieuChamDiemResponse;
import com.example.quanlydetai.entity.DeTai;
import com.example.quanlydetai.entity.DiemThanhPhan;
import com.example.quanlydetai.entity.DotDangKy;
import com.example.quanlydetai.entity.GvhdDeTai;
import com.example.quanlydetai.entity.GvhdDeTaiId;
import com.example.quanlydetai.entity.GvpbDeTai;
import com.example.quanlydetai.entity.GvpbDeTaiId;
import com.example.quanlydetai.entity.HoiDong;
import com.example.quanlydetai.entity.KetQuaDeTai;
import com.example.quanlydetai.entity.ThanhVienHoiDong;
import com.example.quanlydetai.entity.ThanhVienHoiDongId;
import com.example.quanlydetai.exception.BusinessException;
import com.example.quanlydetai.notification.ResultEmailService;
import com.example.quanlydetai.repository.DiemQueryRepository;
import com.example.quanlydetai.repository.JpaEntityRepository;
import com.example.quanlydetai.security.CurrentUserService;

import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.*;
import java.util.*;

@Service
@Validated
@Transactional(readOnly = true)
public class DiemService {
    private final CurrentUserService currentUser;
    private final JpaEntityRepository entities;
    private final AcademicRules rules;
    private final BaoCaoService reports;
    private final ResultEmailService emails;
    private final DiemQueryRepository queries;

    public DiemService(
            CurrentUserService currentUser,
            JpaEntityRepository entities,
            AcademicRules rules,
            BaoCaoService reports,
            ResultEmailService emails,
            DiemQueryRepository queries) {
        this.queries = queries;
        this.currentUser = currentUser;
        this.entities = entities;
        this.rules = rules;
        this.reports = reports;
        this.emails = emails;
    }

    public static BigDecimal average(List<BigDecimal> scores) {
        if (scores == null
                || scores.isEmpty()
                || scores.stream()
                        .anyMatch(
                                s ->
                                        s == null
                                                || s.compareTo(BigDecimal.ZERO) < 0
                                                || s.compareTo(BigDecimal.TEN) > 0))
            throw new com.example.quanlydetai.exception.BusinessException(
                    "Điểm phải từ 0 đến 10 và không được thiếu.");
        return scores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('GV')")
    public void submit(@jakarta.validation.Valid DiemRequest f) {
        String gv = currentUser.teacher();
        String period =
                entities
                        .query(
                                "select t.maDot from DeTai t where t.maDeTai=?1",
                                String.class,
                                f.topic())
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("Không tìm thấy đề tài."));
        entities.lock(DotDangKy.class, period);
        DeTai t = entities.lock(DeTai.class, f.topic());
        rules.check(
                "DangThucHien".equals(t.getTrangThai())
                        && entities.find(KetQuaDeTai.class, f.topic()) == null,
                "Điểm đã khóa sau khi tổng hợp kết quả.");
        rules.check(
                entities.find(GvhdDeTai.class, new GvhdDeTaiId(gv, f.topic())) == null,
                "GVHD không được chấm đề tài đang hướng dẫn.");
        authorize(gv, t, f.type());
        rules.check(
                queries.assignmentsComplete(f.topic()),
                "Cần phân công đủ GVPB và hội đồng đã chốt trước khi chấm điểm.");
        rules.check(reports.hasForTopic(f.topic()), "Nhóm chưa nộp báo cáo để chấm.");
        var previous =
                entities.query(
                        "select d from DiemThanhPhan d where d.maGV=?1 and d.maDeTai=?2 and"
                                + " d.maLoaiDiem=?3",
                        DiemThanhPhan.class,
                        gv,
                        f.topic(),
                        f.type());
        DiemThanhPhan d;
        if (previous.isEmpty()) {
            d = new DiemThanhPhan();
            d.setMaDiem(rules.id("D"));
            d.setMaGV(gv);
            d.setMaDeTai(f.topic());
            d.setMaLoaiDiem(f.type());
            d.setNgayNopDiem(rules.now());
        } else d = previous.getFirst();
        d.setDiem(f.value());
        d.setNhanXet(f.comment().strip());
        d.setNgayCham(rules.now());
        if (previous.isEmpty()) entities.insert(d);
        else entities.flush();
    }

    @Transactional
    @PreAuthorize("hasRole('GV')")
    public void aggregate(String topic, String comment) {
        String gv = currentUser.teacher();
        DeTai t = entities.lock(DeTai.class, topic);
        String council = council(topic);
        rules.check(
                council != null
                        && "DaChot".equals(entities.require(HoiDong.class, council).getTrangThai()),
                "Đề tài chưa có hội đồng đã chốt.");
        var member = entities.find(ThanhVienHoiDong.class, new ThanhVienHoiDongId(council, gv));
        if (member == null || !"ChuTich".equals(member.getVaiTro()))
            throw new AccessDeniedException("Chỉ chủ tịch hội đồng của đề tài được tổng hợp.");
        rules.check(entities.find(KetQuaDeTai.class, topic) == null, "Đề tài đã được tổng hợp.");
        rules.check(
                comment != null && !comment.isBlank() && comment.length() <= 5000,
                "Nhập nhận xét tổng hợp (tối đa 5.000 ký tự).");
        var ballots =
                entities.query(
                        "select d from DiemThanhPhan d where d.maDeTai=?1",
                        DiemThanhPhan.class,
                        topic);
        var reviewers =
                entities.query(
                        "select r from GvpbDeTai r where r.maDeTai=?1", GvpbDeTai.class, topic);
        var members =
                entities.query(
                        "select m from ThanhVienHoiDong m where m.maHd=?1",
                        ThanhVienHoiDong.class,
                        council);
        rules.check(
                reviewers.size() == 1 && members.size() >= 3 && members.size() <= 5,
                "Thiếu phân công phản biện hoặc hội đồng.");
        Set<String> expected = new HashSet<>();
        expected.add(reviewers.getFirst().getMaGV() + ":GVPB");
        members.forEach(m -> expected.add(m.getMaGV() + ":HD"));
        Set<String> actual = new HashSet<>();
        ballots.forEach(d -> actual.add(d.getMaGV() + ":" + d.getMaLoaiDiem()));
        rules.check(
                expected.equals(actual) && ballots.size() == expected.size(),
                "Chưa đủ điểm của GVPB và mọi thành viên hội đồng.");
        rules.check(reports.hasForTopic(topic), "Thiếu báo cáo được dùng để chấm.");
        KetQuaDeTai k = new KetQuaDeTai();
        k.setMaDeTai(topic);
        k.setDiemCuoi(average(ballots.stream().map(DiemThanhPhan::getDiem).toList()));
        k.setNhanXetTongHop(comment.strip());
        k.setMaGvTongHop(gv);
        k.setTrangThaiCongBo("ChuaCongBo");
        k.setNgayTongHop(rules.now());
        entities.insert(k);
        t.setTrangThai("HoanThanh");
        entities.flush();
    }

    @Transactional
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public void publish(String topic) {
        currentUser.dean();
        DeTai t = entities.lock(DeTai.class, topic);
        KetQuaDeTai k = entities.lock(KetQuaDeTai.class, topic);
        rules.check("ChuaCongBo".equals(k.getTrangThaiCongBo()), "Kết quả đã công bố.");
        k.setTrangThaiCongBo("DaCongBo");
        k.setNgayCongBo(rules.now());
        entities.flush();
        emails.enqueue(t, k);
    }

    @PreAuthorize("hasRole('GV')")
    public List<PhieuChamDiemResponse> tasks() {
        return tasks(Paging.defaults()).getContent();
    }

    @PreAuthorize("hasRole('GV')")
    public Page<PhieuChamDiemResponse> tasks(Pageable page) {
        Paging.check(page);
        var rows = queries.tasks(currentUser.teacher(), page);
        var ids = rows.stream().map(r -> (String) r[0]).toList();
        var latestReports = reports.latestForTopics(ids);
        var now = rules.now();
        return rows.map(
                r -> {
                    boolean reviewer = r[3] != null;
                    var deadline = (java.time.LocalDateTime) r[8];
                    boolean locked =
                            r[7] != null
                                    || (reviewer && deadline != null && now.isAfter(deadline))
                                    || !"DangThucHien".equals(r[2]);
                    return new PhieuChamDiemResponse(
                            (String) r[0],
                            (String) r[1],
                            reviewer ? "GVPB" : "HD",
                            (BigDecimal) r[5],
                            r[6] == null ? "" : (String) r[6],
                            locked,
                            (Boolean) r[11],
                            "ChuTich".equals(r[4]),
                            ((Number) r[9]).intValue(),
                            ((Number) r[10]).intValue(),
                            latestReports.getOrDefault((String) r[0], List.of()));
                });
    }

    @PreAuthorize("hasRole('SV')")
    public List<KetQuaResponse> mine() {
        return mine(null);
    }

    @PreAuthorize("hasRole('SV')")
    public List<KetQuaResponse> mine(String period) {
        return mine(period, Paging.defaults()).getContent();
    }

    @PreAuthorize("hasRole('SV')")
    public Page<KetQuaResponse> mine(String period, Pageable page) {
        Paging.check(page);
        String student = currentUser.student();
        String selected = period == null ? "" : period.strip();
        if (!selected.isEmpty() && !queries.registeredInPeriod(student, selected))
            throw new AccessDeniedException("Bạn chưa đăng ký đề tài trong đợt này.");
        return queries.results(student, selected, page);
    }

    public record ResultPeriod(String id, String name) {}

    @PreAuthorize("hasRole('SV')")
    public List<ResultPeriod> myResultPeriods() {
        return entities
                .query(
                        "select d from DotDangKy d where d.maDot in (select n.maDot from Nhom n"
                            + " where n.maDeTai is not null and n.maNhom in (select m.maNhom from"
                            + " ThanhVienNhom m where m.maSV=?1)) order by d.beginSV desc",
                        DotDangKy.class,
                        currentUser.student())
                .stream()
                .map(d -> new ResultPeriod(d.getMaDot(), d.getTenDotDangKy()))
                .toList();
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public List<KetQuaResponse> allResults() {
        return allResults(Paging.defaults()).getContent();
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public Page<KetQuaResponse> allResults(Pageable page) {
        Paging.check(page);
        currentUser.dean();
        return queries.results(null, null, page);
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public Page<KetQuaResponse> pendingPublication(com.example.quanlydetai.dto.TopicSearch filter, Pageable page) {
        Paging.check(page);
        currentUser.dean();
        return queries.pendingPublication(filter == null ? com.example.quanlydetai.dto.TopicSearch.empty() : filter, page);
    }

    private String council(String topic) {
        var h =
                entities.query(
                        "select h.maHd from HoiDongDeTai h where h.maDeTai=?1",
                        String.class,
                        topic);
        return h.isEmpty() ? null : h.getFirst();
    }

    private void authorize(String gv, DeTai t, String type) {
        if ("GVPB".equals(type)) {
            if (entities.find(GvpbDeTai.class, new GvpbDeTaiId(gv, t.getMaDeTai())) == null)
                throw new AccessDeniedException("Bạn không phải GVPB của đề tài.");
            DotDangKy d = entities.require(DotDangKy.class, t.getMaDot());
            rules.reviewerDeadline(d);
            return;
        }
        String council = council(t.getMaDeTai());
        if (!"HD".equals(type)
                || council == null
                || !"DaChot".equals(entities.require(HoiDong.class, council).getTrangThai())
                || entities.find(ThanhVienHoiDong.class, new ThanhVienHoiDongId(council, gv))
                        == null)
            throw new AccessDeniedException("Bạn không thuộc hội đồng đã chốt của đề tài.");
    }
}
