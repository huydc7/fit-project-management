package com.example.quanlydetai.service;

import com.example.quanlydetai.dto.DotDangKyForm;
import com.example.quanlydetai.dto.DotDangKyView;
import com.example.quanlydetai.entity.DotDangKy;
import com.example.quanlydetai.exception.BusinessException;
import com.example.quanlydetai.repository.DotDangKyRepository;

import jakarta.persistence.EntityManager;

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

    public DotDangKyService(
            DotDangKyRepository repository,
            BusinessSecurityService security,
            EntityManager entityManager) {
        this.repository = repository;
        this.security = security;
        this.entityManager = entityManager;
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    public List<DotDangKyView> list() {
        return repository.findAllByOrderByBeginGVDesc().stream()
                .map(
                        d ->
                                new DotDangKyView(
                                        d.getMaDot(),
                                        d.getTenDotDangKy(),
                                        d.getLoai(),
                                        d.getBeginGV(),
                                        d.getEndGV(),
                                        d.getBeginSV(),
                                        d.getEndSV()))
                .toList();
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
            boolean isKLTN = f.getLoai().equals("KLTN");
            if (isKLTN && f.getTimeBcHd() == null) {
                loi.add("Đợt loại KLTN bắt buộc phải có ngày báo cáo hội đồng.");
            }
            if (!isKLTN && f.getTimeBcHd() != null) {
                loi.add("Chỉ đợt loại KLTN mới được đặt ngày báo cáo hội đồng.");
            }
        }
        return loi;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
