package com.example.quanlydetai.service;

import com.example.quanlydetai.dto.BaoCaoResponse;
import com.example.quanlydetai.entity.BaoCao;
import com.example.quanlydetai.entity.DeTai;
import com.example.quanlydetai.entity.GvhdDeTai;
import com.example.quanlydetai.entity.GvhdDeTaiId;
import com.example.quanlydetai.entity.GvpbDeTai;
import com.example.quanlydetai.entity.GvpbDeTaiId;
import com.example.quanlydetai.entity.KetQuaDeTai;
import com.example.quanlydetai.entity.Nhom;
import com.example.quanlydetai.entity.ThanhVienNhom;
import com.example.quanlydetai.entity.ThanhVienNhomId;
import com.example.quanlydetai.repository.BaoCaoRepository;
import com.example.quanlydetai.repository.JpaEntityRepository;
import com.example.quanlydetai.security.CurrentUserService;
import com.example.quanlydetai.storage.ReportStorageService;

import org.slf4j.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class BaoCaoService {
    private static final Logger log = LoggerFactory.getLogger(BaoCaoService.class);
    private final CurrentUserService currentUser;
    private final JpaEntityRepository entities;
    private final AcademicRules rules;
    private final ReportStorageService storage;

    private final BaoCaoRepository repository;

    public BaoCaoService(
            CurrentUserService currentUser,
            JpaEntityRepository entities,
            AcademicRules rules,
            ReportStorageService storage,
            BaoCaoRepository repository) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.entities = entities;
        this.rules = rules;
        this.storage = storage;
    }

    @Transactional
    @PreAuthorize("hasRole('SV')")
    public void submit(String group, String token, MultipartFile file) {
        String sv = currentUser.student();
        Nhom initial = entities.require(Nhom.class, group);
        rules.check(initial.getMaDeTai() != null, "Nhóm chưa được duyệt đề tài.");
        DeTai t = entities.lock(DeTai.class, initial.getMaDeTai());
        Nhom n = entities.lock(Nhom.class, group);
        var m = entities.require(ThanhVienNhom.class, new ThanhVienNhomId(group, sv));
        if (!"Truong_Nhom".equals(m.getVaiTro()))
            throw new AccessDeniedException("Chỉ nhóm trưởng được nộp báo cáo.");
        rules.check(
                token != null && token.matches("[0-9a-f]{24}"),
                "Mã lần nộp không hợp lệ. Hãy tải lại trang.");
        BaoCao previous = entities.find(BaoCao.class, token);
        if (previous != null) {
            rules.check(
                    group.equals(previous.getMaNhom()) && sv.equals(previous.getMaSvNguoiNop()),
                    "Mã lần nộp đã được sử dụng.");
            return;
        }
        rules.check("DangThucHien".equals(t.getTrangThai()), "Đề tài không còn nhận báo cáo.");
        rules.check(
                !entities.exists(
                                "select d.maDiem from DiemThanhPhan d where d.maDeTai=?1",
                                t.getMaDeTai())
                        && entities.find(KetQuaDeTai.class, t.getMaDeTai()) == null,
                "Đã bắt đầu chấm điểm; bản báo cáo dùng để chấm đã bị khóa.");
        String reference = storage.upload(file);
        // A flush may succeed while COMMIT later fails. Compensate only after transaction outcome
        // is known.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK)
                            try {
                                storage.delete(reference);
                            } catch (RuntimeException ex) {
                                log.error("Orphan report object requires cleanup: {}", reference);
                            }
                    }
                });
        int version = repository.lastVersion(group) + 1;
        BaoCao b = new BaoCao();
        b.setMaBc(token);
        b.setMaNhom(group);
        b.setMaSvNguoiNop(sv);
        b.setFileDinhKem(reference);
        b.setLanNop(version);
        b.setNgayNop(rules.now());
        entities.insert(b);
    }

    @PreAuthorize("hasRole('SV')")
    public List<BaoCaoResponse> mine() {
        return mine(Paging.defaults()).getContent();
    }

    @PreAuthorize("hasRole('SV')")
    public Page<BaoCaoResponse> mine(Pageable page) {
        Paging.check(page);
        return repository.mine(currentUser.student(), page);
    }

    @PreAuthorize("hasRole('GV')")
    public List<BaoCaoResponse> assigned() {
        return assigned(Paging.defaults()).getContent();
    }

    @PreAuthorize("hasRole('GV')")
    public Page<BaoCaoResponse> assigned(Pageable page) {
        Paging.check(page);
        String gv = currentUser.teacher();
        return repository.assigned(gv, currentUser.isDean(gv), page);
    }

    public ReportStorageService.Download download(String id) {
        BaoCao b = entities.require(BaoCao.class, id);
        authorize(entities.require(Nhom.class, b.getMaNhom()));
        return storage.download(b.getFileDinhKem());
    }

    private void authorize(Nhom group) {
        var actor = currentUser.actor();
        if (actor.getMaSV() != null
                && entities.find(
                                ThanhVienNhom.class,
                                new ThanhVienNhomId(group.getMaNhom(), actor.getMaSV()))
                        != null) return;
        String gv = actor.getMaGV();
        String topic = group.getMaDeTai();
        if (gv != null
                && (currentUser.isDean(gv)
                        || entities.find(GvhdDeTai.class, new GvhdDeTaiId(gv, topic)) != null
                        || entities.find(GvpbDeTai.class, new GvpbDeTaiId(gv, topic)) != null
                        || entities.exists(
                                "select m.maGV from ThanhVienHoiDong m where m.maGV=?1 and m.maHd"
                                    + " in (select h.maHd from HoiDongDeTai h where h.maDeTai=?2)",
                                gv,
                                topic))) return;
        throw new AccessDeniedException("Bạn không được xem báo cáo của nhóm này.");
    }

    boolean hasForTopic(String topic) {
        return repository.existsByNhom_MaDeTai(topic);
    }

    Map<String, List<BaoCaoResponse>> latestForTopics(List<String> topics) {
        var result = new HashMap<String, List<BaoCaoResponse>>();
        if (!topics.isEmpty())
            for (var b : repository.latestForTopics(topics))
                result.computeIfAbsent(b.topic(), ignored -> new ArrayList<>()).add(b);
        return result;
    }
}
