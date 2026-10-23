package com.example.quanlydetai.repository;

import com.example.quanlydetai.entity.*;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class CatalogQueryRepository extends QuerySupport {
    public Page<DotDangKy> periods(String keyword, Pageable page) {
        var params = new HashMap<String, Object>();
        String from =
                "from DotDangKy d where "
                        + contains(keyword, params, "d.maDot", "d.tenDotDangKy", "d.loai");
        return page(
                "select d",
                from,
                "d.beginGV desc, d.maDot",
                "count(d)",
                DotDangKy.class,
                params,
                page);
    }

    /** Merge public announcements with existing rejected registrations; no notification rows are added. */
    public Page<Object[]> noticeFeed(String keyword, String account, Pageable page) {
        var publicParams = new HashMap<String, Object>();
        String publicFrom = "from ThongBao t where "
                + contains(keyword, publicParams, "t.tieuDe", "t.noiDung");
        var privateParams = new HashMap<String, Object>();
        privateParams.put("account", account);
        String privateFrom = "from DangKyDeTai r, Nhom n, DeTai d where r.maNhom=n.maNhom"
                + " and r.maDeTai=d.maDeTai and r.trangThai='TuChoi' and r.ngayDuyet is not null"
                + " and exists (select m.maSV from ThanhVienNhom m, TaiKhoan a"
                + " where m.maNhom=n.maNhom and m.vaiTro='Truong_Nhom'"
                + " and m.maSV=a.maSV and a.maTk=:account and a.vaiTro='SV') and "
                + contains(keyword, privateParams, "r.lyDo", "n.tenNhom", "n.maDot", "d.tenDeTai",
                        "'Đăng ký đề tài của nhóm bị từ chối'");
        // Parentheses keep ORDER BY on the whole feed, rather than only the final UNION branch.
        String union = "(select t.maTb as id,t.tieuDe,t.noiDung,t.phamVi,t.ngayDang as posted,t.maTk,false "
                + publicFrom + " union all select concat('registration:',r.id),"
                + "'Đăng ký đề tài của nhóm bị từ chối',"
                + "concat('Nhóm: ',n.tenNhom,' — Đợt: ',n.maDot,' — Đề tài: ',d.tenDeTai,' — Lý do: ',coalesce(r.lyDo,'')),"
                + "'Riêng cho nhóm trưởng',r.ngayDuyet,cast(null as String),true " + privateFrom
                + ") order by 5 desc,1";
        var query = em.createQuery(union, Object[].class);
        var publicCount = em.createQuery("select count(t) " + publicFrom, Long.class);
        var privateCount = em.createQuery("select count(r) " + privateFrom, Long.class);
        publicParams.forEach((key, value) -> { query.setParameter(key,value); publicCount.setParameter(key,value); });
        privateParams.forEach((key, value) -> { query.setParameter(key,value); privateCount.setParameter(key,value); });
        long total = publicCount.getSingleResult() + privateCount.getSingleResult();
        var rows = query.setFirstResult(Math.toIntExact(page.getOffset())).setMaxResults(page.getPageSize()).getResultList();
        return new PageImpl<>(rows,page,total);
    }

    public Page<BoMon> departments(String keyword, Pageable page) {
        var params = new HashMap<String, Object>();
        String from = "from BoMon d where " + contains(keyword, params, "d.maBoMon", "d.tenBoMon");
        return page(
                "select d", from, "d.tenBoMon, d.maBoMon", "count(d)", BoMon.class, params, page);
    }
}
