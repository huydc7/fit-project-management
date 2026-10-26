package com.example.quanlydetai.repository;

import com.example.quanlydetai.dto.*;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class NhomQueryRepository extends QuerySupport {
    private static final String REGISTRATION =
            "select new com.example.quanlydetai.dto.DangKyDeTaiResponse(r.id, r.maNhom, n.tenNhom,"
                    + " r.maDeTai, t.tenDeTai, r.trangThai, r.ngayDangKy, r.lyDo)";
    private static final String REGISTRATION_FROM =
            "from DangKyDeTai r join Nhom n on n.maNhom=r.maNhom join DeTai t on"
                    + " t.maDeTai=r.maDeTai";

    public List<NhomOption> registrationOptions(String student, List<String> periods) {
        if (periods.isEmpty()) return List.of();
        return em.createQuery(
                        "select new"
                            + " com.example.quanlydetai.dto.NhomOption(n.maNhom,n.tenNhom,p.maDot,p.tenDotDangKy,n.maDeTai,t.tenDeTai)"
                            + " from Nhom n join n.dot p left join n.deTai t where p.maDot in"
                            + " :periods and n.maDeTai is null and exists (select m.maSV from"
                            + " ThanhVienNhom m where m.maNhom=n.maNhom and m.maSV=:student and"
                            + " m.vaiTro='Truong_Nhom') and not exists (select r.id from"
                            + " DangKyDeTai r where r.activeGroup=n.maNhom) order by n.maNhom",
                        NhomOption.class)
                .setParameter("student", student)
                .setParameter("periods", periods)
                .getResultList();
    }

    public List<NhomOption> reportOptions(String student) {
        return em.createQuery(
                        "select new"
                            + " com.example.quanlydetai.dto.NhomOption(n.maNhom,n.tenNhom,p.maDot,p.tenDotDangKy,t.maDeTai,t.tenDeTai)"
                            + " from Nhom n join n.dot p join n.deTai t where"
                            + " t.trangThai='DangThucHien' and exists (select m.maSV from"
                            + " ThanhVienNhom m where m.maNhom=n.maNhom and m.maSV=:student and"
                            + " m.vaiTro='Truong_Nhom') and not exists (select d.maDiem from"
                            + " DiemThanhPhan d where d.maDeTai=t.maDeTai) and not exists (select"
                            + " k.maDeTai from KetQuaDeTai k where k.maDeTai=t.maDeTai) order by"
                            + " p.beginSV desc,n.maNhom",
                        NhomOption.class)
                .setParameter("student", student)
                .getResultList();
    }

    public Page<Object[]> mine(String student, Pageable page) {
        return page(
                "select n, p.tenDotDangKy, t.tenDeTai, p.beginSV, p.endSV",
                "from Nhom n join n.dot p left join n.deTai t where exists (select m.maSV from"
                        + " ThanhVienNhom m where m.maNhom=n.maNhom and m.maSV=:student)",
                "n.maNhom",
                "count(n)",
                Object[].class,
                Map.of("student", student),
                page);
    }

    public Set<String> activeGroups(List<String> ids) {
        return new HashSet<>(
                em.createQuery(
                                "select r.activeGroup from DangKyDeTai r where r.activeGroup in"
                                        + " :ids",
                                String.class)
                        .setParameter("ids", ids)
                        .getResultList());
    }

    public List<Object[]> members(List<String> ids) {
        return em.createQuery(
                        "select m.maNhom, s.maSV, s.hoTen, m.vaiTro from ThanhVienNhom m join"
                                + " m.sinhVien s where m.maNhom in :ids order by m.maNhom, m.maSV",
                        Object[].class)
                .setParameter("ids", ids)
                .getResultList();
    }

    public List<DangKyDeTaiResponse> registrations(List<String> ids) {
        return em.createQuery(
                        REGISTRATION
                                + " "
                                + REGISTRATION_FROM
                                + " where r.maNhom in :ids order by r.ngayDangKy desc, r.id",
                        DangKyDeTaiResponse.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    public Page<LoiMoiResponse> invitations(String student, Pageable page) {
        return page(
                "select new com.example.quanlydetai.dto.LoiMoiResponse(m.id, m.maNhom, n.tenNhom,"
                        + " m.maSV, s.hoTen, m.trangThai)",
                "from LoiMoiNhom m join Nhom n on n.maNhom=m.maNhom join SinhVien s on"
                        + " s.maSV=m.maSV where m.maSV=:student or exists (select t.maSV from"
                        + " ThanhVienNhom t where t.maNhom=m.maNhom and t.maSV=:student and"
                        + " t.vaiTro='Truong_Nhom')",
                "m.ngayMoi desc, m.id",
                "count(m)",
                LoiMoiResponse.class,
                Map.of("student", student),
                page);
    }

    public Page<DangKyDeTaiResponse> pending(Pageable page) {
        return page(
                REGISTRATION,
                REGISTRATION_FROM + " where r.trangThai='ChoDuyet'",
                "r.ngayDangKy, r.id",
                "count(r)",
                DangKyDeTaiResponse.class,
                Map.of(),
                page);
    }
}
