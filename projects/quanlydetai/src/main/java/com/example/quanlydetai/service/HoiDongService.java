package com.example.quanlydetai.service;

import com.example.quanlydetai.dto.HoiDongRequest;
import com.example.quanlydetai.dto.HoiDongResponse;
import com.example.quanlydetai.dto.ThanhVienResponse;
import com.example.quanlydetai.entity.DeTai;
import com.example.quanlydetai.entity.DotDangKy;
import com.example.quanlydetai.entity.GvhdDeTai;
import com.example.quanlydetai.entity.GvhdDeTaiId;
import com.example.quanlydetai.entity.GvpbDeTai;
import com.example.quanlydetai.entity.GvpbDeTaiId;
import com.example.quanlydetai.entity.HoiDong;
import com.example.quanlydetai.entity.HoiDongDeTai;
import com.example.quanlydetai.entity.KetQuaDeTai;
import com.example.quanlydetai.entity.ThanhVienHoiDong;
import com.example.quanlydetai.repository.HoiDongQueryRepository;
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
public class HoiDongService {
    private final CurrentUserService currentUser;
    private final JpaEntityRepository entities;
    private final AcademicRules rules;
    private final GiangVienService teachers;
    private final HoiDongQueryRepository queries;

    public HoiDongService(
            CurrentUserService currentUser,
            JpaEntityRepository entities,
            AcademicRules rules,
            GiangVienService teachers,
            HoiDongQueryRepository queries) {
        this.queries = queries;
        this.currentUser = currentUser;
        this.entities = entities;
        this.rules = rules;
        this.teachers = teachers;
    }

    @Transactional
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public void create(@jakarta.validation.Valid HoiDongRequest f) {
        currentUser.dean();
        entities.lock(DotDangKy.class, f.period());
        List<String> all = new ArrayList<>(f.members());
        all.add(f.chair());
        all.add(f.secretary());
        rules.check(
                new HashSet<>(all).size() == all.size(),
                "Chủ tịch, thư ký và các thành viên phải là những người khác nhau.");
        teachers.requireActiveAccounts(all);
        HoiDong h = new HoiDong();
        h.setMaHd(rules.id("HD"));
        h.setMaDot(f.period());
        h.setTrangThai("DangLap");
        entities.insert(h);
        for (String gv : all) {
            ThanhVienHoiDong m = new ThanhVienHoiDong();
            m.setMaHd(h.getMaHd());
            m.setMaGV(gv);
            m.setVaiTro(
                    gv.equals(f.chair())
                            ? "ChuTich"
                            : gv.equals(f.secretary()) ? "ThuKy" : "ThanhVien");
            entities.insert(m);
        }
    }

    @Transactional
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public void finalizeCouncil(String id) {
        currentUser.dean();
        HoiDong h = entities.lock(HoiDong.class, id);
        rules.check("DangLap".equals(h.getTrangThai()), "Hội đồng đã chốt.");
        var members = members(id);
        rules.check(
                members.size() >= 3
                        && members.size() <= 5
                        && members.stream().filter(m -> "ChuTich".equals(m.getVaiTro())).count()
                                == 1
                        && members.stream().filter(m -> "ThuKy".equals(m.getVaiTro())).count() == 1,
                "Hội đồng cần 3–5 người, đúng một chủ tịch và một thư ký.");
        teachers.requireActiveAccounts(members.stream().map(ThanhVienHoiDong::getMaGV).toList());
        h.setTrangThai("DaChot");
        entities.flush();
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public HoiDongRequest editForm(String id) {
        currentUser.dean();
        var h = entities.require(HoiDong.class, id);
        requireEditable(h);
        var rows = members(id);
        return new HoiDongRequest(
                h.getMaDot(),
                rows.stream()
                        .filter(m -> "ChuTich".equals(m.getVaiTro()))
                        .findFirst()
                        .map(ThanhVienHoiDong::getMaGV)
                        .orElse(null),
                rows.stream()
                        .filter(m -> "ThuKy".equals(m.getVaiTro()))
                        .findFirst()
                        .map(ThanhVienHoiDong::getMaGV)
                        .orElse(null),
                rows.stream()
                        .filter(m -> "ThanhVien".equals(m.getVaiTro()))
                        .map(ThanhVienHoiDong::getMaGV)
                        .toList());
    }

    @Transactional
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public void update(String id, @jakarta.validation.Valid HoiDongRequest f) {
        var h = editableCouncil(id);
        rules.check(
                h.getMaDot().equals(f.period()),
                "Không đổi đợt của hội đồng; hãy lập hội đồng mới nếu cần.");
        List<String> all = new ArrayList<>(f.members());
        all.add(f.chair());
        all.add(f.secretary());
        rules.check(
                new HashSet<>(all).size() == all.size(),
                "Chủ tịch, thư ký và các thành viên phải là những người khác nhau.");
        teachers.requireActiveAccounts(all);
        members(id).forEach(entities::remove);
        entities.flush();
        for (String gv : all) {
            var m = new ThanhVienHoiDong();
            m.setMaHd(id);
            m.setMaGV(gv);
            m.setVaiTro(
                    gv.equals(f.chair())
                            ? "ChuTich"
                            : gv.equals(f.secretary()) ? "ThuKy" : "ThanhVien");
            entities.insert(m);
        }
    }

    @Transactional
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public void deleteDraft(String id) {
        var h = editableCouncil(id);
        members(id).forEach(entities::remove);
        entities.flush();
        entities.remove(h);
        entities.flush();
    }

    private HoiDong editableCouncil(String id) {
        currentUser.dean();
        String period =
                entities
                        .query("select h.maDot from HoiDong h where h.maHd=?1", String.class, id)
                        .stream()
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new com.example.quanlydetai.exception.BusinessException(
                                                "Không tìm thấy hội đồng."));
        entities.lock(DotDangKy.class, period);
        var h = entities.lock(HoiDong.class, id);
        requireEditable(h);
        return h;
    }

    private void requireEditable(HoiDong h) {
        rules.check(
                "DangLap".equals(h.getTrangThai())
                        && !entities.exists(
                                "select r from HoiDongDeTai r where r.maHd=?1", h.getMaHd()),
                "Chỉ sửa/xóa hội đồng đang lập và chưa phân công đề tài.");
    }

    @Transactional
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public void assignReviewer(String topic, String teacher) {
        currentUser.dean();
        DeTai t = lockTopic(topic);
        ungraded(t);
        teachers.requireActiveAccounts(List.of(teacher));
        rules.check(
                entities.find(GvhdDeTai.class, new GvhdDeTaiId(teacher, topic)) == null,
                "GVHD không được phản biện đề tài đang hướng dẫn.");
        rules.check(
                !entities.exists(
                        "select m.maGV from ThanhVienHoiDong m where m.maGV=?1 and m.maHd in"
                                + " (select h.maHd from HoiDongDeTai h where h.maDeTai=?2)",
                        teacher,
                        topic),
                "GVPB phải độc lập với hội đồng của đề tài.");
        var previous =
                entities.query(
                        "select p from GvpbDeTai p where p.maDeTai=?1", GvpbDeTai.class, topic);
        if (!previous.isEmpty() && previous.getFirst().getMaGV().equals(teacher)) return;
        previous.forEach(entities::remove);
        entities.flush();
        GvpbDeTai p = new GvpbDeTai();
        p.setMaGV(teacher);
        p.setMaDeTai(topic);
        p.setNgayPhanCong(rules.now());
        entities.insert(p);
    }

    @Transactional
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public void assignCouncil(String topic, String council) {
        currentUser.dean();
        DeTai t = lockTopic(topic);
        ungraded(t);
        HoiDong h = entities.lock(HoiDong.class, council);
        rules.check(
                "DaChot".equals(h.getTrangThai()),
                "Hãy chốt thành viên hội đồng trước khi gán đề tài.");
        rules.check(t.getMaDot().equals(h.getMaDot()), "Hội đồng và đề tài phải cùng đợt.");
        var councilMembers = members(council);
        teachers.requireActiveAccounts(
                councilMembers.stream().map(ThanhVienHoiDong::getMaGV).toList());
        for (var m : councilMembers) {
            rules.check(
                    entities.find(GvhdDeTai.class, new GvhdDeTaiId(m.getMaGV(), topic)) == null,
                    "Hội đồng có thành viên là GVHD của đề tài.");
            rules.check(
                    entities.find(GvpbDeTai.class, new GvpbDeTaiId(m.getMaGV(), topic)) == null,
                    "Hội đồng có thành viên đã là GVPB của đề tài.");
        }
        var previous =
                entities.query(
                        "select r from HoiDongDeTai r where r.maDeTai=?1",
                        HoiDongDeTai.class,
                        topic);
        if (!previous.isEmpty() && previous.getFirst().getMaHd().equals(council)) return;
        previous.forEach(entities::remove);
        entities.flush();
        HoiDongDeTai link = new HoiDongDeTai();
        link.setMaHd(council);
        link.setMaDeTai(topic);
        entities.insert(link);
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public List<HoiDongResponse> all() {
        return all(Paging.defaults()).getContent();
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public Page<HoiDongResponse> all(Pageable page) {
        Paging.check(page);
        currentUser.dean();
        var rows = queries.all(page);
        if (rows.isEmpty())
            return rows.map(
                    h ->
                            new HoiDongResponse(
                                    h.getMaHd(),
                                    h.getDot().getTenDotDangKy(),
                                    h.getTrangThai(),
                                    List.of(),
                                    List.of()));
        var ids = rows.stream().map(HoiDong::getMaHd).toList();
        var members = new HashMap<String, List<ThanhVienResponse>>();
        for (var row : queries.members(ids))
            members.computeIfAbsent((String) row[0], ignored -> new ArrayList<>())
                    .add(new ThanhVienResponse((String) row[1], (String) row[2], (String) row[3]));
        var topics = new HashMap<String, List<String>>();
        for (var row : queries.topics(ids))
            topics.computeIfAbsent((String) row[0], ignored -> new ArrayList<>())
                    .add((String) row[1]);
        return rows.map(
                h ->
                        new HoiDongResponse(
                                h.getMaHd(),
                                h.getDot().getTenDotDangKy(),
                                h.getTrangThai(),
                                members.getOrDefault(h.getMaHd(), List.of()),
                                topics.getOrDefault(h.getMaHd(), List.of())));
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public List<HoiDongResponse> optionsForPeriods(List<String> periods) {
        currentUser.dean();
        return queries.options(periods).stream()
                .map(
                        h ->
                                new HoiDongResponse(
                                        h.getMaHd(),
                                        h.getDot().getTenDotDangKy(),
                                        h.getTrangThai(),
                                        List.of(),
                                        List.of()))
                .toList();
    }

    private List<ThanhVienHoiDong> members(String id) {
        return entities.query(
                "select m from ThanhVienHoiDong m where m.maHd=?1", ThanhVienHoiDong.class, id);
    }

    private DeTai lockTopic(String id) {
        DeTai t = entities.require(DeTai.class, id);
        entities.lock(DotDangKy.class, t.getMaDot());
        return entities.lock(DeTai.class, id);
    }

    private void ungraded(DeTai t) {
        rules.check(
                "DangThucHien".equals(t.getTrangThai()),
                "Chỉ phân công cho đề tài đã có nhóm được duyệt.");
        rules.check(
                entities.find(KetQuaDeTai.class, t.getMaDeTai()) == null
                        && !entities.exists(
                                "select d.maDiem from DiemThanhPhan d where d.maDeTai=?1",
                                t.getMaDeTai()),
                "Phân công đã khóa khi bắt đầu chấm điểm.");
    }
}
