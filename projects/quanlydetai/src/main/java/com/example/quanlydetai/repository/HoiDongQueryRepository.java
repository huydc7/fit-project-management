package com.example.quanlydetai.repository;

import com.example.quanlydetai.entity.HoiDong;
import com.example.quanlydetai.dto.HoiDongTopicOption;
import com.example.quanlydetai.dto.LuaChonResponse;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class HoiDongQueryRepository extends QuerySupport {
    public List<HoiDongTopicOption> topicOptions() {
        return em.createQuery(
                        "select t.maDeTai,t.tenDeTai,t.maDot,d.tenDotDangKy from DeTai t, DotDangKy d"
                                + " where t.maDot=d.maDot and t.trangThai='DangThucHien'"
                                + " and not exists (select k.maDeTai from KetQuaDeTai k where k.maDeTai=t.maDeTai)"
                                + " and not exists (select s.maDiem from DiemThanhPhan s where s.maDeTai=t.maDeTai)"
                                + " order by d.beginSV desc,t.tenDeTai,t.maDeTai", Object[].class)
                .getResultList().stream()
                .map(r -> new HoiDongTopicOption((String) r[0], r[1] + " — " + r[3] + " (" + r[0] + ")", (String) r[2]))
                .toList();
    }

    public List<LuaChonResponse> candidates(String topic) {
        return em.createQuery(
                        "select g.maGV,g.hoTen from GiangVien g where exists"
                                + " (select a.maTk from TaiKhoan a where a.maGV=g.maGV"
                                + " and a.vaiTro='GV' and a.trangThai='HoatDong')"
                                + " and not exists (select h.maGV from GvhdDeTai h"
                                + " where h.maDeTai=:topic and h.maGV=g.maGV)"
                                + " and not exists (select p.maGV from GvpbDeTai p"
                                + " where p.maDeTai=:topic and p.maGV=g.maGV) order by g.hoTen,g.maGV", Object[].class)
                .setParameter("topic", topic).getResultList().stream()
                .map(r -> new LuaChonResponse((String) r[0], r[1] + " — " + r[0])).toList();
    }

    public List<Object[]> eligibleForTopics(List<String> topics) {
        if (topics.isEmpty()) return List.of();
        return em.createQuery(
                        "select t.maDeTai,h.maHd,h.dot.tenDotDangKy from HoiDong h, DeTai t"
                                + " where t.maDeTai in :topics and h.maDot=t.maDot"
                                + " and h.trangThai='DaChot' and not exists"
                                + " (select m.maGV from ThanhVienHoiDong m where m.maHd=h.maHd"
                                + " and (exists (select g.maGV from GvhdDeTai g"
                                + " where g.maDeTai=t.maDeTai and g.maGV=m.maGV)"
                                + " or exists (select p.maGV from GvpbDeTai p"
                                + " where p.maDeTai=t.maDeTai and p.maGV=m.maGV)"
                                + " or not exists (select a.maTk from TaiKhoan a"
                                + " where a.maGV=m.maGV and a.vaiTro='GV'"
                                + " and a.trangThai='HoatDong'))) order by t.maDeTai,h.maHd",
                        Object[].class)
                .setParameter("topics", topics)
                .getResultList();
    }
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

}
