package com.example.quanlydetai.service;

import com.example.quanlydetai.dto.ThongBaoRequest;
import com.example.quanlydetai.dto.ThongBaoResponse;
import com.example.quanlydetai.entity.TaiKhoan;
import com.example.quanlydetai.entity.ThongBao;
import com.example.quanlydetai.repository.JpaEntityRepository;
import com.example.quanlydetai.security.CurrentUserService;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@Transactional(readOnly = true)
public class ThongBaoService {
    private final CurrentUserService currentUser;
    private final JpaEntityRepository entities;
    private final AcademicRules rules;

    public ThongBaoService(
            CurrentUserService currentUser, JpaEntityRepository entities, AcademicRules rules) {
        this.currentUser = currentUser;
        this.entities = entities;
        this.rules = rules;
    }

    public List<ThongBaoResponse> list() {
        currentUser.actor();
        return entities
                .query("select t from ThongBao t order by t.ngayDang desc", ThongBao.class)
                .stream()
                .map(
                        t ->
                                new ThongBaoResponse(
                                        t.getMaTb(),
                                        t.getTieuDe(),
                                        t.getNoiDung(),
                                        t.getPhamVi(),
                                        t.getNgayDang()))
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','TRUONGKHOA')")
    public void create(@jakarta.validation.Valid ThongBaoRequest f) {
        var actor = currentUser.actor();
        var account = entities.require(TaiKhoan.class, actor.getAccountId());
        boolean admin = "Admin".equals(account.getVaiTro());
        if ("NhaTruong".equals(f.scope()) && !admin)
            throw new AccessDeniedException("Chỉ Admin được đăng thông báo nhà trường.");
        if (!admin) currentUser.dean();
        ThongBao t = new ThongBao();
        t.setMaTb(rules.id("TB"));
        t.setMaTk(actor.getAccountId());
        t.setTieuDe(f.title().strip());
        t.setNoiDung(f.content().strip());
        t.setPhamVi(f.scope());
        t.setNgayDang(rules.now());
        entities.insert(t);
    }
}
