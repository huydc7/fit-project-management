package com.example.quanlydetai.repository;

import com.example.quanlydetai.entity.TaiKhoan;
import com.example.quanlydetai.entity.TruongBoMon;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class TaiKhoanQueryRepository extends QuerySupport {
    public Set<String> deans(List<String> ids) {
        if (ids.isEmpty()) return Set.of();
        return new HashSet<>(
                em.createQuery(
                                "select d.maGV from TruongKhoa d where d.dangGiuChuc=true and"
                                        + " d.maGV in :ids",
                                String.class)
                        .setParameter("ids", ids)
                        .getResultList());
    }

    public List<TruongBoMon> heads(List<String> ids) {
        if (ids.isEmpty()) return List.of();
        return em.createQuery(
                        "select h from TruongBoMon h where h.dangGiuChuc=true and h.maGV in :ids",
                        TruongBoMon.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    public Page<TaiKhoan> search(String keyword, String role, Pageable page) {
        var params = new HashMap<String, Object>();
        var from =
                new StringBuilder(
                        "from TaiKhoan a left join fetch a.sinhVien s left join fetch a.giangVien g"
                                + " where ");
        from.append(
                contains(
                        keyword,
                        params,
                        "a.tenDangNhap",
                        "a.maSV",
                        "a.maGV",
                        "s.hoTen",
                        "g.hoTen",
                        "s.email",
                        "g.email"));
        equal(from, params, "a.vaiTro", "role", role);
        return page(
                "select a",
                from.toString(),
                "a.tenDangNhap, a.maTk",
                "count(a)",
                TaiKhoan.class,
                params,
                page);
    }
}
