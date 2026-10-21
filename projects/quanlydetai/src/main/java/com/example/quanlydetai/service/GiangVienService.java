package com.example.quanlydetai.service;

import com.example.quanlydetai.dto.KhoaTaiKhoanResponse.Task;
import com.example.quanlydetai.entity.DeTai;
import com.example.quanlydetai.entity.GiangVien;
import com.example.quanlydetai.entity.HoiDong;
import com.example.quanlydetai.entity.TaiKhoan;
import com.example.quanlydetai.repository.JpaEntityRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class GiangVienService {
    private final JpaEntityRepository entities;
    private final AcademicRules rules;
    @PersistenceContext private EntityManager em;

    public GiangVienService(JpaEntityRepository entities, AcademicRules rules) {
        this.entities = entities;
        this.rules = rules;
    }

    @Transactional
    public void requireActiveAccounts(Collection<String> teachers) {
        rules.check(
                teachers.stream().allMatch(id -> id != null && !id.isBlank()),
                "Mã giảng viên không hợp lệ.");
        // A common lock order also serializes assignment with Admin account locking.
        for (String teacher : teachers.stream().distinct().sorted().toList()) {
            entities.require(GiangVien.class, teacher);
            var accounts =
                    entities.query(
                            "select a.maTk from TaiKhoan a where a.maGV=?1 and a.vaiTro='GV'",
                            String.class,
                            teacher);
            rules.check(
                    accounts.size() == 1,
                    "Giảng viên " + teacher + " chưa có tài khoản GV để nhận phân công.");
            TaiKhoan account = entities.lock(TaiKhoan.class, accounts.getFirst());
            // Refresh a previously loaded actor too, after any competing lock operation commits.
            em.refresh(account, LockModeType.PESSIMISTIC_WRITE);
            rules.check(
                    "HoatDong".equals(account.getTrangThai()),
                    "Tài khoản giảng viên " + teacher + " đã bị khóa; hãy chọn GV đang hoạt động.");
        }
    }

    public List<Task> pendingTasks(String teacher) {
        List<Task> tasks = new ArrayList<>();
        for (var t :
                entities.query(
                        "select t from DeTai t where t.trangThai in"
                            + " ('Nhap','DaCongBo','DangThucHien') and exists (select g.maGV from"
                            + " GvhdDeTai g where g.maDeTai=t.maDeTai and g.maGV=?1) order by"
                            + " t.maDeTai",
                        DeTai.class,
                        teacher)) {
            tasks.add(
                    new Task(
                            "GVHD",
                            t.getMaDeTai(),
                            t.getTenDeTai(),
                            null,
                            "Trao đổi với Trưởng bộ môn/Trưởng khoa. Đề tài đã công bố chưa hỗ trợ"
                                + " đổi GVHD trực tiếp; mở khóa tài khoản để tiếp tục hướng dẫn khi"
                                + " phù hợp."));
        }
        for (var t :
                entities.query(
                        "select t from DeTai t where t.trangThai='DangThucHien' and not exists"
                            + " (select k.maDeTai from KetQuaDeTai k where k.maDeTai=t.maDeTai) and"
                            + " exists (select g.maGV from GvpbDeTai g where g.maDeTai=t.maDeTai"
                            + " and g.maGV=?1) and not exists (select d.maDiem from DiemThanhPhan d"
                            + " where d.maDeTai=t.maDeTai and d.maGV=?1 and d.maLoaiDiem='GVPB')"
                            + " order by t.maDeTai",
                        DeTai.class,
                        teacher)) {
            tasks.add(
                    new Task(
                            "GVPB",
                            t.getMaDeTai(),
                            t.getTenDeTai(),
                            null,
                            "Trưởng khoa phân công lại nếu đề tài chưa có phiếu điểm. Khi đã bắt"
                                + " đầu chấm, phân công bị khóa; cần mở khóa tài khoản để hoàn tất,"
                                + " không xóa phiếu đã chấm."));
        }
        for (var row :
                entities.query(
                        "select t.maDeTai,t.tenDeTai,m.maHd,m.vaiTro from DeTai t, HoiDongDeTai r,"
                            + " ThanhVienHoiDong m where r.maDeTai=t.maDeTai and m.maHd=r.maHd and"
                            + " m.maGV=?1 and t.trangThai='DangThucHien' and not exists (select"
                            + " k.maDeTai from KetQuaDeTai k where k.maDeTai=t.maDeTai) and"
                            + " (m.vaiTro='ChuTich' or not exists (select d.maDiem from"
                            + " DiemThanhPhan d where d.maDeTai=t.maDeTai and d.maGV=?1 and"
                            + " d.maLoaiDiem='HD')) order by t.maDeTai",
                        Object[].class,
                        teacher)) {
            tasks.add(
                    new Task(
                            "ChuTich".equals(row[3])
                                    ? "Chủ tịch hội đồng (chấm/tổng hợp)"
                                    : "Thành viên hội đồng",
                            (String) row[0],
                            (String) row[1],
                            (String) row[2],
                            "Trưởng khoa có thể đổi hội đồng trước khi có phiếu điểm. Hội đồng đã"
                                + " chốt không sửa thành viên; khi đã chấm, mở khóa tài khoản để"
                                + " hoàn tất."));
        }
        for (var h :
                entities.query(
                        "select h from HoiDong h where exists (select m.maGV from ThanhVienHoiDong"
                            + " m where m.maHd=h.maHd and m.maGV=?1) and not exists (select r.maHd"
                            + " from HoiDongDeTai r where r.maHd=h.maHd) order by h.maHd",
                        HoiDong.class,
                        teacher)) {
            tasks.add(
                    new Task(
                            "Hội đồng",
                            null,
                            "Hội đồng chưa phân công đề tài",
                            h.getMaHd(),
                            "DangLap".equals(h.getTrangThai())
                                    ? "Trưởng khoa sửa thành viên hội đồng đang lập trước khi chốt."
                                    : "Hội đồng đã chốt: dùng hội đồng khác có thành viên hoạt động"
                                            + " hoặc mở khóa tài khoản trước khi gán đề tài."));
        }
        return List.copyOf(tasks);
    }
}
