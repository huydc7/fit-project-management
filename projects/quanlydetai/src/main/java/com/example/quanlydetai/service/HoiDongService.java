package com.example.quanlydetai.service;

import com.example.quanlydetai.dto.HoiDongRequest;
import com.example.quanlydetai.dto.HoiDongResponse;
import com.example.quanlydetai.dto.HoiDongTopicOption;
import com.example.quanlydetai.dto.LuaChonResponse;
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

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public void create(@jakarta.validation.Valid HoiDongRequest f) {
        createDraft(f);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public void createForTopic(String topic, @jakarta.validation.Valid HoiDongRequest f) {
        currentUser.dean();
        DeTai t = lockTopic(topic);
        ungraded(t);
        rules.check(t.getMaDot().equals(f.period()), "Đề tài và hội đồng phải cùng đợt.");
        requireIndependent(topic, selectedMembers(f));
        String id = createDraft(f);
        finalizeCouncil(id);
        assignCouncil(topic, id);
    }

    private String createDraft(HoiDongRequest f) {
        currentUser.dean();
        entities.lock(DotDangKy.class, f.period());
        List<String> all = selectedMembers(f);
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
        return h.getMaHd();
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public void finalizeCouncil(String id) {
        HoiDong h = editableCouncil(id);
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

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public void updateForTopic(String id, String topic, @jakarta.validation.Valid HoiDongRequest f) {
        var h = editableCouncil(id);
        rules.check(topic != null && !topic.isBlank(), "Hãy chọn đề tài trước khi sửa thành viên hội đồng.");
        var period = entities.query("select t.maDot from DeTai t where t.maDeTai=?1", String.class, topic);
        rules.check(!period.isEmpty() && h.getMaDot().equals(period.getFirst()), "Đề tài và hội đồng phải cùng đợt.");
        ungraded(entities.lock(DeTai.class, topic));
        requireIndependent(topic, selectedMembers(f));
        update(id, f);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public void update(String id, @jakarta.validation.Valid HoiDongRequest f) {
        var h = editableCouncil(id);
        rules.check(
                h.getMaDot().equals(f.period()),
                "Không đổi đợt của hội đồng; hãy lập hội đồng mới nếu cần.");
        List<String> all = selectedMembers(f);

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

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
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

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
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

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
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
    public Map<String, List<HoiDongResponse>> optionsForTopics(List<String> topics) {
        currentUser.dean();
        var options = new HashMap<String, List<HoiDongResponse>>();
        for (String topic : topics) options.put(topic, new ArrayList<>());
        for (Object[] row : queries.eligibleForTopics(topics))
            options.get((String) row[0]).add(new HoiDongResponse(
                    (String) row[1], (String) row[2], "DaChot", List.of(), List.of()));
        return options;
    }

    private List<ThanhVienHoiDong> members(String id) {
        return entities.query(
                "select m from ThanhVienHoiDong m where m.maHd=?1", ThanhVienHoiDong.class, id);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public void finalizeForTopic(String id, String topic) {
        currentUser.dean();
        rules.check(topic != null && !topic.isBlank(), "Hãy chọn đề tài trước khi chốt hội đồng.");
        DeTai t = lockTopic(topic);
        ungraded(t);
        HoiDong h = editableCouncil(id);
        rules.check(t.getMaDot().equals(h.getMaDot()), "Đề tài và hội đồng phải cùng đợt.");
        requireIndependent(topic, members(id).stream().map(ThanhVienHoiDong::getMaGV).toList());
        finalizeCouncil(id);
        assignCouncil(topic, id);
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public List<HoiDongTopicOption> topicOptions() {
        currentUser.dean();
        return queries.topicOptions();
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public List<LuaChonResponse> candidates(String topic) {
        currentUser.dean();
        ungraded(entities.require(DeTai.class, topic));
        return queries.candidates(topic);
    }

    private List<String> selectedMembers(HoiDongRequest f) {
        List<String> all = new ArrayList<>(f.members());
        all.add(f.chair());
        all.add(f.secretary());
        return all;
    }

    private void requireIndependent(String topic, List<String> members) {
        for (String teacher : members) {
            rules.check(!entities.exists("select h.maGV from GvhdDeTai h where h.maDeTai=?1 and h.maGV=?2", topic, teacher),
                    "GVHD không được làm Chủ tịch, Thư ký hoặc thành viên hội đồng của đề tài đang hướng dẫn.");
            rules.check(!entities.exists("select p.maGV from GvpbDeTai p where p.maDeTai=?1 and p.maGV=?2", topic, teacher),
                    "GVPB của đề tài phải độc lập với hội đồng.");
        }
    }

    private DeTai lockTopic(String id) {
        var period = entities.query("select t.maDot from DeTai t where t.maDeTai=?1", String.class, id);
        rules.check(!period.isEmpty(), "Không tìm thấy đề tài.");
        entities.lock(DotDangKy.class, period.getFirst());
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
