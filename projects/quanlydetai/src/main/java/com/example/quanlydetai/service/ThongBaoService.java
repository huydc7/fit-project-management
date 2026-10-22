package com.example.quanlydetai.service;

import com.example.quanlydetai.dto.ThongBaoRequest;
import com.example.quanlydetai.dto.ThongBaoResponse;
import com.example.quanlydetai.entity.TaiKhoan;
import com.example.quanlydetai.entity.ThongBao;
import com.example.quanlydetai.repository.CatalogQueryRepository;
import com.example.quanlydetai.repository.JpaEntityRepository;
import com.example.quanlydetai.security.CurrentUserService;

import org.springframework.data.domain.*;
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

    private final CatalogQueryRepository queries;

    public ThongBaoService(
            CurrentUserService currentUser,
            JpaEntityRepository entities,
            AcademicRules rules,
            CatalogQueryRepository queries) {
        this.queries = queries;
        this.currentUser = currentUser;
        this.entities = entities;
        this.rules = rules;
    }

    public List<ThongBaoResponse> list() {
        return search("", Paging.defaults()).getContent();
    }

    public List<ThongBaoResponse> recent() {
        currentUser.actor();
        var account = entities.require(TaiKhoan.class, currentUser.actor().getAccountId());
        boolean admin = "Admin".equals(account.getVaiTro());
        boolean dean = account.getMaGV() != null && currentUser.isDean(account.getMaGV());
        return queries.recentNotices().stream()
                .map(t -> view(t, account.getMaTk(), admin, dean))
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

    public List<ThongBaoResponse> search(String q) {
        return search(q, Paging.defaults()).getContent();
    }

    public Page<ThongBaoResponse> search(String q, Pageable page) {
        Paging.check(page);
        var account = entities.require(TaiKhoan.class, currentUser.actor().getAccountId());
        boolean admin = "Admin".equals(account.getVaiTro());
        boolean dean = account.getMaGV() != null && currentUser.isDean(account.getMaGV());
        return queries.notices(q, page).map(t -> view(t, account.getMaTk(), admin, dean));
    }

    private ThongBaoResponse view(ThongBao t, String account, boolean admin, boolean dean) {
        return new ThongBaoResponse(
                t.getMaTb(),
                t.getTieuDe(),
                t.getNoiDung(),
                t.getPhamVi(),
                t.getNgayDang(),
                admin || (dean && "Khoa".equals(t.getPhamVi()) && account.equals(t.getMaTk())));
    }

    private boolean canManage(ThongBao t) {
        var a = entities.require(TaiKhoan.class, currentUser.actor().getAccountId());
        return "Admin".equals(a.getVaiTro())
                || ("Khoa".equals(t.getPhamVi())
                        && a.getMaTk().equals(t.getMaTk())
                        && currentUser.isDean(a.getMaGV()));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','TRUONGKHOA')")
    public void update(String id, @jakarta.validation.Valid ThongBaoRequest f) {
        ThongBao t = entities.lock(ThongBao.class, id);
        if (!canManage(t)) throw new AccessDeniedException("Không được sửa thông báo này.");
        if ("NhaTruong".equals(f.scope())) currentUser.admin();
        t.setTieuDe(f.title().strip());
        t.setNoiDung(f.content().strip());
        t.setPhamVi(f.scope());
        entities.flush();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','TRUONGKHOA')")
    public void delete(String id) {
        ThongBao t = entities.lock(ThongBao.class, id);
        if (!canManage(t)) throw new AccessDeniedException("Không được xóa thông báo này.");
        entities.remove(t);
        entities.flush();
    }
}
