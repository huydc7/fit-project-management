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
import com.example.quanlydetai.entity.Nhom;
import com.example.quanlydetai.entity.ThanhVienHoiDong;
import com.example.quanlydetai.entity.ThanhVienHoiDongId;
import com.example.quanlydetai.exception.BusinessException;
import com.example.quanlydetai.notification.ResultEmailService;
import com.example.quanlydetai.repository.JpaEntityRepository;
import com.example.quanlydetai.security.CurrentUserService;

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

    public DiemService(
            CurrentUserService currentUser,
            JpaEntityRepository entities,
            AcademicRules rules,
            BaoCaoService reports,
            ResultEmailService emails) {
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

    @Transactional
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
        rules.check(!reports.forTopic(f.topic()).isEmpty(), "Nhóm chưa nộp báo cáo để chấm.");
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
        rules.check(!reports.forTopic(topic).isEmpty(), "Thiếu báo cáo được dùng để chấm.");
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
        String gv = currentUser.teacher();
        List<PhieuChamDiemResponse> tasks = new ArrayList<>();
        var topics =
                entities.query(
                        "select t from DeTai t where t.maDeTai in (select p.maDeTai from GvpbDeTai"
                            + " p where p.maGV=?1) or t.maDeTai in (select h.maDeTai from"
                            + " HoiDongDeTai h where h.maHd in (select m.maHd from ThanhVienHoiDong"
                            + " m where m.maGV=?1)) order by t.ngayDangKy desc",
                        DeTai.class,
                        gv);
        for (DeTai t : topics) {
            String topic = t.getMaDeTai(), council = council(topic);
            boolean reviewer = entities.find(GvpbDeTai.class, new GvpbDeTaiId(gv, topic)) != null;
            var member =
                    council == null
                            ? null
                            : entities.find(
                                    ThanhVienHoiDong.class, new ThanhVienHoiDongId(council, gv));
            String type = reviewer ? "GVPB" : "HD";
            var score =
                    entities.query(
                            "select d from DiemThanhPhan d where d.maGV=?1 and d.maDeTai=?2 and"
                                    + " d.maLoaiDiem=?3",
                            DiemThanhPhan.class,
                            gv,
                            topic,
                            type);
            DiemThanhPhan own = score.isEmpty() ? null : score.getFirst();
            int submitted =
                    entities.query(
                                    "select d from DiemThanhPhan d where d.maDeTai=?1",
                                    DiemThanhPhan.class,
                                    topic)
                            .size();
            int required =
                    council == null
                            ? 0
                            : 1
                                    + entities.query(
                                                    "select m from ThanhVienHoiDong m where"
                                                            + " m.maHd=?1",
                                                    ThanhVienHoiDong.class,
                                                    council)
                                            .size();
            DotDangKy dot = entities.require(DotDangKy.class, t.getMaDot());
            boolean expired =
                    reviewer
                            && dot.getDeadlineGvpbNopDiem() != null
                            && rules.now().isAfter(dot.getDeadlineGvpbNopDiem());
            boolean locked =
                    entities.find(KetQuaDeTai.class, topic) != null
                            || expired
                            || !"DangThucHien".equals(t.getTrangThai());
            tasks.add(
                    new PhieuChamDiemResponse(
                            topic,
                            t.getTenDeTai(),
                            type,
                            own == null ? null : own.getDiem(),
                            own == null ? "" : own.getNhanXet(),
                            locked,
                            member != null && "ChuTich".equals(member.getVaiTro()),
                            submitted,
                            required,
                            reports.forTopic(topic)));
        }
        return tasks;
    }

    @PreAuthorize("hasRole('SV')")
    public List<KetQuaResponse> mine() {
        return mine(null);
    }

    @PreAuthorize("hasRole('SV')")
    public List<KetQuaResponse> mine(String period) {
        String sv = currentUser.student();
        String selected = period == null ? "" : period.strip();
        if (!selected.isEmpty()
                && myResultPeriods().stream().noneMatch(p -> p.id().equals(selected))) {
            throw new AccessDeniedException("Bạn chưa đăng ký đề tài trong đợt này.");
        }
        return entities
                .query(
                        "select k from KetQuaDeTai k where k.trangThaiCongBo='DaCongBo' and"
                            + " k.maDeTai in (select n.maDeTai from Nhom n where (?2='' or"
                            + " n.maDot=?2) and n.maNhom in (select m.maNhom from ThanhVienNhom m"
                            + " where m.maSV=?1)) order by k.ngayCongBo desc",
                        KetQuaDeTai.class,
                        sv,
                        selected)
                .stream()
                .map(this::resultView)
                .toList();
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
        currentUser.dean();
        return entities
                .query("select k from KetQuaDeTai k order by k.ngayTongHop desc", KetQuaDeTai.class)
                .stream()
                .map(this::resultView)
                .toList();
    }

    private KetQuaResponse resultView(KetQuaDeTai k) {
        var topic = entities.require(DeTai.class, k.getMaDeTai());
        var period = entities.require(DotDangKy.class, topic.getMaDot());
        var n =
                entities.query(
                        "select n from Nhom n where n.maDeTai=?1", Nhom.class, k.getMaDeTai());
        return new KetQuaResponse(
                k.getMaDeTai(),
                topic.getTenDeTai(),
                n.isEmpty() ? "" : n.getFirst().getTenNhom(),
                k.getDiemCuoi(),
                k.getNhanXetTongHop(),
                k.getTrangThaiCongBo(),
                k.getNgayCongBo(),
                period.getMaDot(),
                period.getTenDotDangKy());
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
