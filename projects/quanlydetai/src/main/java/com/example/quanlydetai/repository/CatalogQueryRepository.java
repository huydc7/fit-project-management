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

    public Page<ThongBao> notices(String keyword, Pageable page) {
        var params = new HashMap<String, Object>();
        String from = "from ThongBao t where " + contains(keyword, params, "t.tieuDe", "t.noiDung");
        return page(
                "select t",
                from,
                "t.ngayDang desc, t.maTb",
                "count(t)",
                ThongBao.class,
                params,
                page);
    }

    public List<ThongBao> recentNotices() {
        return em.createQuery(
                        "select t from ThongBao t order by t.ngayDang desc, t.maTb", ThongBao.class)
                .setMaxResults(3)
                .getResultList();
    }

    public Page<BoMon> departments(String keyword, Pageable page) {
        var params = new HashMap<String, Object>();
        String from = "from BoMon d where " + contains(keyword, params, "d.maBoMon", "d.tenBoMon");
        return page(
                "select d", from, "d.tenBoMon, d.maBoMon", "count(d)", BoMon.class, params, page);
    }
}
