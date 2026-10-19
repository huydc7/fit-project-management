package com.example.quanlydetai.service;

import com.example.quanlydetai.dto.DoiMatKhauRequest;
import com.example.quanlydetai.dto.TaiKhoanRequest;
import com.example.quanlydetai.dto.TaiKhoanResponse;
import com.example.quanlydetai.dto.ThongTinCaNhanResponse;
import com.example.quanlydetai.entity.BoMon;
import com.example.quanlydetai.entity.GiangVien;
import com.example.quanlydetai.entity.SinhVien;
import com.example.quanlydetai.entity.TaiKhoan;
import com.example.quanlydetai.entity.TruongBoMon;
import com.example.quanlydetai.entity.TruongKhoa;
import com.example.quanlydetai.repository.JpaEntityRepository;
import com.example.quanlydetai.security.CurrentUserService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Validated
@Transactional(readOnly = true)
public class TaiKhoanService {
    private final CurrentUserService currentUser;
    private final JpaEntityRepository entities;
    private final AcademicRules rules;
    private final PasswordEncoder encoder;

    public TaiKhoanService(
            CurrentUserService currentUser,
            JpaEntityRepository entities,
            AcademicRules rules,
            PasswordEncoder encoder) {
        this.currentUser = currentUser;
        this.entities = entities;
        this.rules = rules;
        this.encoder = encoder;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<TaiKhoanResponse> list() {
        currentUser.admin();
        var deans =
                entities.query(
                        "select d.maGV from TruongKhoa d where d.dangGiuChuc=true", String.class);
        var departmentHeads =
                entities
                        .query(
                                "select h from TruongBoMon h where h.dangGiuChuc=true",
                                TruongBoMon.class)
                        .stream()
                        .collect(Collectors.toMap(TruongBoMon::getMaBoMon, TruongBoMon::getMaGV));
        return entities
                .query("select a from TaiKhoan a order by a.tenDangNhap", TaiKhoan.class)
                .stream()
                .map(
                        a -> {
                            SinhVien s = entities.find(SinhVien.class, a.getMaSV());
                            GiangVien g = entities.find(GiangVien.class, a.getMaGV());
                            return new TaiKhoanResponse(
                                    a.getMaTk(),
                                    a.getTenDangNhap(),
                                    a.getVaiTro(),
                                    a.getMaSV() != null ? a.getMaSV() : a.getMaGV(),
                                    a.getTrangThai(),
                                    s != null ? s.getHoTen() : g != null ? g.getHoTen() : "",
                                    s != null ? s.getEmail() : g != null ? g.getEmail() : "",
                                    g != null ? g.getMaBoMon() : "",
                                    "GV".equals(a.getVaiTro())
                                            && g != null
                                            && deans.contains(g.getMaGV()),
                                    "GV".equals(a.getVaiTro())
                                            && g != null
                                            && g.getMaGV()
                                                    .equals(departmentHeads.get(g.getMaBoMon())));
                        })
                .toList();
    }

    @PreAuthorize("hasAnyRole('SV', 'GV')")
    public ThongTinCaNhanResponse personalProfile() {
        TaiKhoan account = entities.require(TaiKhoan.class, currentUser.actor().getAccountId());
        if ("SV".equals(account.getVaiTro())) {
            SinhVien student = entities.require(SinhVien.class, currentUser.student());
            return new ThongTinCaNhanResponse(
                    account.getTenDangNhap(),
                    student.getMaSV(),
                    student.getHoTen(),
                    student.getEmail(),
                    "SV",
                    "",
                    false,
                    false);
        }
        GiangVien teacher = entities.require(GiangVien.class, currentUser.teacher());
        BoMon department = entities.require(BoMon.class, teacher.getMaBoMon());
        TruongBoMon head = entities.find(TruongBoMon.class, department.getMaBoMon());
        return new ThongTinCaNhanResponse(
                account.getTenDangNhap(),
                teacher.getMaGV(),
                teacher.getHoTen(),
                teacher.getEmail(),
                "GV",
                department.getTenBoMon(),
                currentUser.isDean(teacher.getMaGV()),
                head != null && head.isDangGiuChuc() && teacher.getMaGV().equals(head.getMaGV()));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void create(@jakarta.validation.Valid TaiKhoanRequest f) {
        currentUser.admin();
        checkPassword(f.password());
        rules.check(
                !entities.exists(
                        "select a.maTk from TaiKhoan a where a.tenDangNhap=?1", f.username()),
                "Tên đăng nhập đã tồn tại.");
        if (!"Admin".equals(f.role())) {
            rules.check(
                    f.profileId() != null && f.profileId().matches("[a-zA-Z0-9_-]{1,20}"),
                    "Mã SV/GV cần 1–20 chữ, số, dấu gạch.");
            rules.check(
                    f.name() != null
                            && !f.name().isBlank()
                            && f.email() != null
                            && !f.email().isBlank(),
                    "Cần họ tên và email cho sinh viên/giảng viên.");
            if ("SV".equals(f.role())) {
                rules.check(
                        entities.find(SinhVien.class, f.profileId()) == null,
                        "Mã sinh viên đã tồn tại.");
                SinhVien s = new SinhVien();
                s.setMaSV(f.profileId());
                s.setHoTen(f.name().strip());
                s.setEmail(f.email().strip());
                entities.insert(s);
            } else {
                rules.check(
                        entities.find(GiangVien.class, f.profileId()) == null,
                        "Mã giảng viên đã tồn tại.");
                entities.require(BoMon.class, f.department());
                GiangVien g = new GiangVien();
                g.setMaGV(f.profileId());
                g.setHoTen(f.name().strip());
                g.setEmail(f.email().strip());
                g.setMaBoMon(f.department());
                entities.insert(g);
            }
        }
        TaiKhoan a = new TaiKhoan();
        a.setMaTk(rules.id("TK"));
        a.setTenDangNhap(f.username());
        a.setMatKhauHash(encoder.encode(f.password()));
        a.setVaiTro(f.role());
        a.setTrangThai("HoatDong");
        a.setNgayTao(rules.now());
        if ("SV".equals(f.role())) a.setMaSV(f.profileId());
        if ("GV".equals(f.role())) a.setMaGV(f.profileId());
        entities.insert(a);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void setActive(String id, boolean active) {
        currentUser.admin();
        TaiKhoan a = entities.lock(TaiKhoan.class, id);
        rules.check(
                !id.equals(currentUser.actor().getAccountId()),
                "Không được khóa tài khoản đang sử dụng.");
        a.setTrangThai(active ? "HoatDong" : "Khoa");
        entities.flush();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void resetPassword(String id, String password) {
        currentUser.admin();
        checkPassword(password);
        TaiKhoan a = entities.lock(TaiKhoan.class, id);
        a.setMatKhauHash(encoder.encode(password));
        entities.flush();
    }

    @Transactional
    public void changePassword(@jakarta.validation.Valid DoiMatKhauRequest f) {
        TaiKhoan a = entities.lock(TaiKhoan.class, currentUser.actor().getAccountId());
        rules.check(
                encoder.matches(f.currentPassword(), a.getMatKhauHash()),
                "Mật khẩu hiện tại không đúng.");
        rules.check(f.newPassword().equals(f.confirmation()), "Mật khẩu xác nhận không khớp.");
        checkPassword(f.newPassword());
        rules.check(
                !encoder.matches(f.newPassword(), a.getMatKhauHash()),
                "Mật khẩu mới cần khác mật khẩu hiện tại.");
        a.setMatKhauHash(encoder.encode(f.newPassword()));
        entities.flush();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void addDepartment(String id, String name) {
        currentUser.admin();
        rules.check(
                id != null
                        && id.matches("[a-zA-Z0-9_-]{1,20}")
                        && name != null
                        && !name.isBlank()
                        && name.length() <= 150,
                "Mã/tên bộ môn chưa hợp lệ.");
        rules.check(entities.find(BoMon.class, id) == null, "Mã bộ môn đã tồn tại.");
        BoMon b = new BoMon();
        b.setMaBoMon(id);
        b.setTenBoMon(name.strip());
        entities.insert(b);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void dean(String teacher, boolean appointed) {
        currentUser.admin();
        entities.lock(GiangVien.class, teacher);
        TruongKhoa existing = entities.find(TruongKhoa.class, teacher);
        if (appointed && existing == null) {
            TruongKhoa d = new TruongKhoa();
            d.setMaGV(teacher);
            entities.insert(d);
        } else if (existing != null) {
            existing.setDangGiuChuc(appointed);
            entities.flush();
        }
    }

    public static void checkPassword(String password) {
        if (password == null
                || password.length() < 12
                || password.length() > 64
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new com.example.quanlydetai.exception.BusinessException(
                    "Mật khẩu cần 12–64 ký tự và không vượt 72 byte UTF-8.");
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<TaiKhoanResponse> search(String q, String role) {
        return list().stream()
                .filter(a -> role == null || role.isBlank() || role.equals(a.role()))
                .filter(a -> SearchText.matches(q, a.username(), a.profile(), a.name(), a.email()))
                .toList();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void updateProfile(
            String id, @jakarta.validation.Valid com.example.quanlydetai.dto.HoSoRequest f) {
        currentUser.admin();
        TaiKhoan a = entities.lock(TaiKhoan.class, id);
        rules.check(!"Admin".equals(a.getVaiTro()), "Tài khoản Admin không có hồ sơ SV/GV.");
        if (a.getMaSV() != null) {
            SinhVien s = entities.lock(SinhVien.class, a.getMaSV());
            s.setHoTen(f.name().strip());
            s.setEmail(f.email().strip());
        } else {
            GiangVien g = entities.lock(GiangVien.class, a.getMaGV());
            entities.require(BoMon.class, f.department());
            var head =
                    entities.find(com.example.quanlydetai.entity.TruongBoMon.class, g.getMaBoMon());
            rules.check(
                    java.util.Objects.equals(g.getMaBoMon(), f.department())
                            || head == null
                            || !head.isDangGiuChuc()
                            || !g.getMaGV().equals(head.getMaGV()),
                    "Cần thu hồi chức vụ Trưởng bộ môn trước khi chuyển bộ môn.");
            g.setHoTen(f.name().strip());
            g.setEmail(f.email().strip());
            g.setMaBoMon(f.department());
        }
        entities.flush();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUnusedAccount(String id) {
        currentUser.admin();
        rules.check(
                !id.equals(currentUser.actor().getAccountId()),
                "Không được xóa tài khoản đang sử dụng.");
        TaiKhoan a = entities.lock(TaiKhoan.class, id);
        Object profile =
                a.getMaSV() != null
                        ? entities.lock(SinhVien.class, a.getMaSV())
                        : a.getMaGV() != null ? entities.lock(GiangVien.class, a.getMaGV()) : null;
        // Foreign keys RESTRICT preserve academic history; any failure rolls back the entire
        // operation.
        entities.remove(a);
        entities.flush();
        if (profile != null) {
            entities.remove(profile);
            entities.flush();
        }
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void updateDepartment(String id, String name) {
        currentUser.admin();
        rules.check(
                name != null && !name.isBlank() && name.length() <= 150,
                "Tên bộ môn cần 1–150 ký tự.");
        entities.lock(BoMon.class, id).setTenBoMon(name.strip());
        entities.flush();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteDepartment(String id) {
        currentUser.admin();
        BoMon d = entities.lock(BoMon.class, id);
        rules.check(
                !entities.exists("select g from GiangVien g where g.maBoMon=?1", id)
                        && !entities.exists("select t from DeTai t where t.maBoMon=?1", id),
                "Bộ môn đang có giảng viên/đề tài; không thể xóa.");
        entities.remove(d);
        entities.flush();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<com.example.quanlydetai.dto.LuaChonResponse> departmentHeads() {
        currentUser.admin();
        return entities
                .query(
                        "select h from TruongBoMon h where h.dangGiuChuc=true",
                        com.example.quanlydetai.entity.TruongBoMon.class)
                .stream()
                .map(
                        h ->
                                new com.example.quanlydetai.dto.LuaChonResponse(
                                        h.getMaBoMon(), h.getMaGV()))
                .toList();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void appointDepartmentHead(
            @jakarta.validation.Valid com.example.quanlydetai.dto.TruongBoMonRequest f) {
        currentUser.admin();
        entities.lock(BoMon.class, f.department());
        GiangVien gv = entities.lock(GiangVien.class, f.teacher());
        rules.check(
                f.department().equals(gv.getMaBoMon()),
                "Giảng viên phải thuộc bộ môn được bổ nhiệm.");
        var head = entities.find(com.example.quanlydetai.entity.TruongBoMon.class, f.department());
        if (f.appointed()) {
            rules.check(
                    entities.exists(
                            "select a from TaiKhoan a where a.maGV=?1 and a.trangThai='HoatDong'",
                            f.teacher()),
                    "Giảng viên cần tài khoản đang hoạt động.");
            if (head == null) {
                head = new com.example.quanlydetai.entity.TruongBoMon();
                head.setMaBoMon(f.department());
                head.setMaGV(f.teacher());
                entities.insert(head);
            } else {
                head.setMaGV(f.teacher());
                head.setDangGiuChuc(true);
            }
        } else {
            rules.check(
                    head != null && f.teacher().equals(head.getMaGV()),
                    "Chức vụ không khớp người được thu hồi.");
            head.setDangGiuChuc(false);
        }
        entities.flush();
    }
}
