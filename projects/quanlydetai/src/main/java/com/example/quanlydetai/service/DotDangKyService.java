package com.example.quanlydetai.service;

import com.example.quanlydetai.dto.DotDangKyForm;
import com.example.quanlydetai.dto.DotDangKyView;
import com.example.quanlydetai.entity.DotDangKy;
import com.example.quanlydetai.exception.BusinessException;
import com.example.quanlydetai.repository.CatalogQueryRepository;
import com.example.quanlydetai.repository.DotDangKyRepository;

import jakarta.persistence.EntityManager;

import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@org.springframework.validation.annotation.Validated
@Transactional(readOnly = true)
public class DotDangKyService {
    private final DotDangKyRepository repository;
    private final BusinessSecurityService security;
    private final EntityManager entityManager;

    private final CatalogQueryRepository queries;

    public DotDangKyService(
            DotDangKyRepository repository,
            BusinessSecurityService security,
            EntityManager entityManager,
            CatalogQueryRepository queries) {
        this.queries = queries;
        this.repository = repository;
        this.security = security;
        this.entityManager = entityManager;
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public List<DotDangKyView> list() {
        return search("", Paging.defaults()).getContent();
    }

    @Transactional
    @PreAuthorize("hasRole('TRUONGKHOA') and #maGV == authentication.principal.maGV")
    public void create(@jakarta.validation.Valid DotDangKyForm form, String maGV) {
        security.requireTruongKhoa(maGV);
        List<String> errors = kiemTra(form);
        if (!errors.isEmpty()) throw new BusinessException(String.join(" ", errors));
        if (repository.existsById(form.getMaDot()))
            throw new BusinessException("Mã đợt đã tồn tại.");
        DotDangKy dot = new DotDangKy();
        dot.setMaDot(form.getMaDot());
        dot.setMaTruongKhoa(maGV);
        dot.setTenDotDangKy(form.getTenDotDangKy());
        dot.setLoai(form.getLoai());
        dot.setBeginGV(form.getBeginGV());
        dot.setEndGV(form.getEndGV());
        dot.setBeginSV(form.getBeginSV());
        dot.setEndSV(form.getEndSV());
        dot.setDeadlineGvpbNopDiem(form.getDeadlineGvpbNopDiem());
        dot.setTimeBcHd(form.getTimeBcHd());
        // INSERT only: repository.save would merge an assigned ID during a concurrent duplicate
        // submission.
        entityManager.persist(dot);
        entityManager.flush();
    }

    private List<String> kiemTra(DotDangKyForm f) {
        List<String> loi = new ArrayList<>();
        if (isBlank(f.getMaDot())) loi.add("Mã đợt không được để trống.");
        if (isBlank(f.getTenDotDangKy())) loi.add("Tên đợt không được để trống.");
        if (isBlank(f.getLoai())) loi.add("Phải chọn loại đợt.");

        if (f.getBeginGV() != null
                && f.getEndGV() != null
                && !f.getBeginGV().isBefore(f.getEndGV())) {
            loi.add("Thời gian bắt đầu GV đăng ký phải trước thời gian kết thúc.");
        }
        if (f.getBeginSV() != null
                && f.getEndSV() != null
                && !f.getBeginSV().isBefore(f.getEndSV())) {
            loi.add("Thời gian bắt đầu SV đăng ký phải trước thời gian kết thúc.");
        }
        if (f.getEndGV() != null
                && f.getBeginSV() != null
                && f.getEndGV().isAfter(f.getBeginSV())) {
            loi.add("Giai đoạn SV đăng ký phải bắt đầu sau khi giai đoạn GV đăng ký kết thúc.");
        }

        if (f.getLoai() != null) {
            boolean canDeadlineGvpb = f.getLoai().equals("TLCN") || f.getLoai().equals("KLTN");
            if (canDeadlineGvpb && f.getDeadlineGvpbNopDiem() == null) {
                loi.add("Đợt loại TLCN/KLTN bắt buộc phải có hạn chót GVPB nộp điểm.");
            }
            if (!canDeadlineGvpb && f.getDeadlineGvpbNopDiem() != null) {
                loi.add("Đợt MonHoc/NCKH không được đặt hạn chót GVPB nộp điểm.");
            }
            if (canDeadlineGvpb
                    && f.getDeadlineGvpbNopDiem() != null
                    && f.getEndSV() != null
                    && !f.getDeadlineGvpbNopDiem().isAfter(f.getEndSV())) {
                loi.add("Hạn chót GVPB nộp điểm phải sau thời gian kết thúc đăng ký của sinh viên.");
            }
            boolean isKLTN = f.getLoai().equals("KLTN");
            if (isKLTN && f.getTimeBcHd() == null) {
                loi.add("Đợt loại KLTN bắt buộc phải có ngày báo cáo hội đồng.");
            }
            if (!isKLTN && f.getTimeBcHd() != null) {
                loi.add("Chỉ đợt loại KLTN mới được đặt ngày báo cáo hội đồng.");
            }
            if (isKLTN
                    && f.getTimeBcHd() != null
                    && f.getEndSV() != null
                    && !f.getTimeBcHd().isAfter(f.getEndSV())) {
                loi.add("Ngày báo cáo hội đồng phải sau thời gian kết thúc đăng ký của sinh viên.");
            }
            if (isKLTN
                    && f.getDeadlineGvpbNopDiem() != null
                    && f.getTimeBcHd() != null
                    && !f.getDeadlineGvpbNopDiem().isBefore(f.getTimeBcHd())) {
                loi.add("Hạn chót GVPB nộp điểm phải trước thời điểm báo cáo hội đồng.");
            }
        }
        return loi;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public List<DotDangKyView> search(String q) {
        return search(q, Paging.defaults()).getContent();
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public Page<DotDangKyView> search(String q, Pageable page) {
        Paging.check(page);
        return queries.periods(q, page)
                .map(
                        d ->
                                new DotDangKyView(
                                        d.getMaDot(),
                                        d.getTenDotDangKy(),
                                        d.getLoai(),
                                        d.getBeginGV(),
                                        d.getEndGV(),
                                        d.getBeginSV(),
                                        d.getEndSV()));
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public DotDangKyForm editForm(String id) {
        DotDangKy d = entityManager.find(DotDangKy.class, id);
        if (d == null) throw new BusinessException("Không tìm thấy đợt.");
        DotDangKyForm f = new DotDangKyForm();
        f.setMaDot(d.getMaDot());
        f.setTenDotDangKy(d.getTenDotDangKy());
        f.setLoai(d.getLoai());
        f.setBeginGV(d.getBeginGV());
        f.setEndGV(d.getEndGV());
        f.setBeginSV(d.getBeginSV());
        f.setEndSV(d.getEndSV());
        f.setDeadlineGvpbNopDiem(d.getDeadlineGvpbNopDiem());
        f.setTimeBcHd(d.getTimeBcHd());
        return f;
    }

    @Transactional
    @PreAuthorize("hasRole('TRUONGKHOA') and #maGV == authentication.principal.maGV")
    public void update(String id, @jakarta.validation.Valid DotDangKyForm f, String maGV) {
        security.requireTruongKhoa(maGV);
        if (!id.equals(f.getMaDot())) throw new BusinessException("Không được thay đổi mã đợt.");
        var errors = kiemTra(f);
        if (!errors.isEmpty()) throw new BusinessException(String.join(" ", errors));
        DotDangKy d =
                entityManager.find(
                        DotDangKy.class, id, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (d == null) throw new BusinessException("Không tìm thấy đợt.");
        boolean occupied = hasActivity(id);
        if (occupied
                && (!java.util.Objects.equals(d.getLoai(), f.getLoai())
                        || !sameMinute(d.getBeginGV(), f.getBeginGV())
                        || !sameMinute(d.getEndGV(), f.getEndGV())
                        || !sameMinute(d.getBeginSV(), f.getBeginSV())
                        || !sameMinute(d.getTimeBcHd(), f.getTimeBcHd())))
            throw new BusinessException(
                    "Đợt đã có dữ liệu: không đổi loại, mốc GV, bắt đầu SV hoặc ngày hội đồng; chỉ"
                            + " được sửa tên và gia hạn.");
        if (occupied) {
            boolean extended =
                    !sameMinute(d.getEndSV(), f.getEndSV())
                            || !sameMinute(d.getDeadlineGvpbNopDiem(), f.getDeadlineGvpbNopDiem());
            if (f.getEndSV()
                            .isBefore(
                                    d.getEndSV().truncatedTo(java.time.temporal.ChronoUnit.MINUTES))
                    || (d.getDeadlineGvpbNopDiem() != null
                            && (f.getDeadlineGvpbNopDiem() == null
                                    || f.getDeadlineGvpbNopDiem()
                                            .isBefore(
                                                    d.getDeadlineGvpbNopDiem()
                                                            .truncatedTo(
                                                                    java.time.temporal.ChronoUnit
                                                                            .MINUTES)))))
                throw new BusinessException("Không được rút ngắn hạn của đợt đã có dữ liệu.");
            if (extended
                    && !entityManager
                            .createQuery(
                                    "select k from KetQuaDeTai k where k.maDeTai in (select"
                                            + " t.maDeTai from DeTai t where t.maDot=:id)")
                            .setParameter("id", id)
                            .setMaxResults(1)
                            .getResultList()
                            .isEmpty())
                throw new BusinessException("Đợt đã có kết quả tổng hợp; chỉ được sửa tên.");
        }
        d.setTenDotDangKy(f.getTenDotDangKy().strip());
        if (occupied) {
            if (!sameMinute(d.getEndSV(), f.getEndSV())) d.setEndSV(f.getEndSV());
            if (!sameMinute(d.getDeadlineGvpbNopDiem(), f.getDeadlineGvpbNopDiem()))
                d.setDeadlineGvpbNopDiem(f.getDeadlineGvpbNopDiem());
            entityManager.flush();
            return;
        }
        d.setLoai(f.getLoai());
        d.setBeginGV(f.getBeginGV());
        d.setEndGV(f.getEndGV());
        d.setBeginSV(f.getBeginSV());
        d.setEndSV(f.getEndSV());
        d.setDeadlineGvpbNopDiem(f.getDeadlineGvpbNopDiem());
        d.setTimeBcHd(f.getTimeBcHd());
        entityManager.flush();
    }

    @Transactional
    @PreAuthorize("hasRole('TRUONGKHOA') and #maGV == authentication.principal.maGV")
    public void delete(String id, String maGV) {
        security.requireTruongKhoa(maGV);
        DotDangKy d =
                entityManager.find(
                        DotDangKy.class, id, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (d == null) throw new BusinessException("Không tìm thấy đợt.");
        if (hasActivity(id))
            throw new BusinessException("Đợt đã có đề tài/nhóm/hội đồng; không thể xóa.");
        entityManager.remove(d);
        entityManager.flush();
    }

    private boolean sameMinute(java.time.LocalDateTime a, java.time.LocalDateTime b) {
        return a == null
                ? b == null
                : b != null
                        && a.truncatedTo(java.time.temporal.ChronoUnit.MINUTES)
                                .equals(b.truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
    }

    private boolean hasActivity(String id) {
        for (String type : List.of("DeTai", "Nhom", "HoiDong"))
            if (!entityManager
                    .createQuery("select e from " + type + " e where e.maDot=:id")
                    .setParameter("id", id)
                    .setMaxResults(1)
                    .getResultList()
                    .isEmpty()) return true;
        return false;
    }
}
