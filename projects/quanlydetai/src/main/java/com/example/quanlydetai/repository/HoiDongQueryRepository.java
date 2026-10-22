package com.example.quanlydetai.repository;

import com.example.quanlydetai.entity.HoiDong;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class HoiDongQueryRepository extends QuerySupport {
    public Page<HoiDong> all(Pageable page) {
        return page(
                "select h",
                "from HoiDong h join fetch h.dot",
                "h.maHd",
                "count(h)",
                HoiDong.class,
                Map.of(),
                page);
    }

    public List<Object[]> members(List<String> ids) {
        return em.createQuery(
                        "select m.maHd, g.maGV, g.hoTen, m.vaiTro from ThanhVienHoiDong m join"
                                + " m.giangVien g where m.maHd in :ids order by m.maHd, m.maGV",
                        Object[].class)
                .setParameter("ids", ids)
                .getResultList();
    }

    public List<Object[]> topics(List<String> ids) {
        return em.createQuery(
                        "select l.maHd, t.tenDeTai from HoiDongDeTai l join l.deTai t where l.maHd"
                                + " in :ids order by l.maHd, t.maDeTai",
                        Object[].class)
                .setParameter("ids", ids)
                .getResultList();
    }

    public List<HoiDong> options(List<String> periods) {
        if (periods.isEmpty()) return List.of();
        return em.createQuery(
                        "select h from HoiDong h join fetch h.dot where h.trangThai='DaChot' and"
                                + " h.maDot in :periods order by h.maHd",
                        HoiDong.class)
                .setParameter("periods", periods)
                .getResultList();
    }
}
