package com.example.quanlydetai.repository;

import com.example.quanlydetai.dto.KetQuaResponse;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class DiemQueryRepository extends QuerySupport {
    public Page<Object[]> tasks(String teacher, Pageable page) {
        String from =
                "from DeTai t join t.dot p"
                        + " left join GvpbDeTai r on r.maDeTai=t.maDeTai and r.maGV=:teacher"
                        + " left join HoiDongDeTai h on h.maDeTai=t.maDeTai"
                        + " left join ThanhVienHoiDong m on m.maHd=h.maHd and m.maGV=:teacher"
                        + " left join DiemThanhPhan d on d.maDeTai=t.maDeTai and d.maGV=:teacher"
                        + " and d.maLoaiDiem=case when r.maGV is not null then 'GVPB' else 'HD' end"
                        + " left join KetQuaDeTai k on k.maDeTai=t.maDeTai"
                        + " where r.maGV is not null or m.maGV is not null";
        String select =
                "select t.maDeTai, t.tenDeTai, t.trangThai, r.maGV, m.vaiTro, d.diem, d.nhanXet,"
                        + " k.maDeTai, p.deadlineGvpbNopDiem, (select count(s) from DiemThanhPhan s"
                        + " where s.maDeTai=t.maDeTai), case when h.maHd is null then 0 else 1 +"
                        + " (select count(c) from ThanhVienHoiDong c where c.maHd=h.maHd) end";
        return page(
                select,
                from,
                "t.ngayDangKy desc, t.maDeTai",
                "count(t)",
                Object[].class,
                Map.of("teacher", teacher),
                page);
    }

    public Page<KetQuaResponse> results(String student, String period, Pageable page) {
        var params = new HashMap<String, Object>();
        var from =
                new StringBuilder(
                        "from KetQuaDeTai k join k.deTai t join t.dot p left join Nhom n on"
                                + " n.maDeTai=t.maDeTai where 1=1");
        if (student != null) {
            from.append(
                    " and k.trangThaiCongBo='DaCongBo' and exists (select m.maSV from ThanhVienNhom"
                            + " m where m.maNhom=n.maNhom and m.maSV=:student)");
            params.put("student", student);
        }
        equal(from, params, "p.maDot", "period", period);
        return page(
                "select new com.example.quanlydetai.dto.KetQuaResponse(k.maDeTai, t.tenDeTai,"
                    + " coalesce(n.tenNhom,''), k.diemCuoi, k.nhanXetTongHop, k.trangThaiCongBo,"
                    + " k.ngayCongBo, p.maDot, p.tenDotDangKy)",
                from.toString(),
                student == null ? "k.ngayTongHop desc, k.maDeTai" : "k.ngayCongBo desc, k.maDeTai",
                "count(k)",
                KetQuaResponse.class,
                params,
                page);
    }

    public boolean registeredInPeriod(String student, String period) {
        return !em.createQuery(
                        "select n.maNhom from Nhom n where n.maDot=:period and n.maDeTai is not"
                                + " null and exists (select m.maSV from ThanhVienNhom m where"
                                + " m.maNhom=n.maNhom and m.maSV=:student)",
                        String.class)
                .setParameter("student", student)
                .setParameter("period", period)
                .setMaxResults(1)
                .getResultList()
                .isEmpty();
    }
}
