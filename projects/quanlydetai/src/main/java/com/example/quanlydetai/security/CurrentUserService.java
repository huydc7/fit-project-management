package com.example.quanlydetai.security;

import com.example.quanlydetai.entity.TaiKhoan;
import com.example.quanlydetai.entity.TruongKhoa;
import com.example.quanlydetai.repository.JpaEntityRepository;


import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


/** Actor IDs come exclusively from the authenticated session. */
@Component
public class CurrentUserService {
    private final JpaEntityRepository entities;

    public CurrentUserService(JpaEntityRepository entities) {
        this.entities = entities;
    }

    public AccountPrincipal actor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AccountPrincipal a))
            throw new AccessDeniedException("Vui lòng đăng nhập.");
        TaiKhoan account = entities.require(TaiKhoan.class, a.getAccountId());
        if (!"HoatDong".equals(account.getTrangThai()))
            throw new AccessDeniedException("Tài khoản đã bị khóa.");
        return a;
    }

    public String student() {
        String id = actor().getMaSV();
        if (id == null) throw new AccessDeniedException("Chỉ sinh viên được thực hiện.");
        return id;
    }

    public String teacher() {
        String id = actor().getMaGV();
        if (id == null) throw new AccessDeniedException("Chỉ giảng viên được thực hiện.");
        return id;
    }

    public String dean() {
        String id = teacher();
        if (!isDean(id)) throw new AccessDeniedException("Chỉ Trưởng khoa được thực hiện.");
        return id;
    }

    public boolean isDean(String gv) {
        TruongKhoa d = gv == null ? null : entities.find(TruongKhoa.class, gv);
        return d != null && d.isDangGiuChuc();
    }

    public void admin() {
        if (!"Admin".equals(entities.require(TaiKhoan.class, actor().getAccountId()).getVaiTro()))
            throw new AccessDeniedException("Chỉ quản trị viên được thực hiện.");
    }

    public String departmentHead() {
        String teacher = teacher();
        var g = entities.require(com.example.quanlydetai.entity.GiangVien.class, teacher);
        var head = entities.find(com.example.quanlydetai.entity.TruongBoMon.class, g.getMaBoMon());
        if (head == null || !head.isDangGiuChuc() || !teacher.equals(head.getMaGV()))
            throw new AccessDeniedException("Chỉ Trưởng bộ môn đương nhiệm được thực hiện.");
        return g.getMaBoMon();
    }
}
