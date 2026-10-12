package com.example.quanlydetai.service;

import com.example.quanlydetai.entity.GvhdDeTaiId;
import com.example.quanlydetai.entity.ThanhVienHoiDongId;
import com.example.quanlydetai.entity.ThanhVienNhomId;
import com.example.quanlydetai.repository.GvhdDeTaiRepository;
import com.example.quanlydetai.repository.ThanhVienHoiDongRepository;
import com.example.quanlydetai.repository.ThanhVienNhomRepository;
import com.example.quanlydetai.repository.TruongKhoaRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Shared policy predicates. Acting user IDs must come from AccountPrincipal. */
@Service
@Transactional(readOnly = true)
public class BusinessSecurityService {
    private final TruongKhoaRepository deans;
    private final ThanhVienHoiDongRepository members;
    private final GvhdDeTaiRepository supervisors;
    private final ThanhVienNhomRepository students;

    public BusinessSecurityService(
            TruongKhoaRepository deans,
            ThanhVienHoiDongRepository members,
            GvhdDeTaiRepository supervisors,
            ThanhVienNhomRepository students) {
        this.deans = deans;
        this.members = members;
        this.supervisors = supervisors;
        this.students = students;
    }

    public boolean isTruongKhoa(String maGV) {
        return maGV != null && deans.findById(maGV).map(d -> d.isDangGiuChuc()).orElse(false);
    }

    public boolean isChuTichHoiDong(String maGV, String maHd) {
        return maGV != null
                && maHd != null
                && members.findById(new ThanhVienHoiDongId(maHd, maGV))
                        .map(member -> "ChuTich".equals(member.getVaiTro()))
                        .orElse(false);
    }

    public boolean isGVHD(String maGV, String maDeTai) {
        return maGV != null
                && maDeTai != null
                && supervisors.existsById(new GvhdDeTaiId(maGV, maDeTai));
    }

    public boolean isTruongNhom(String maSV, String maNhom) {
        return maSV != null
                && maNhom != null
                && students.findById(new ThanhVienNhomId(maNhom, maSV))
                        .map(member -> "Truong_Nhom".equals(member.getVaiTro()))
                        .orElse(false);
    }

    public void requireTruongKhoa(String maGV) {
        if (!isTruongKhoa(maGV))
            throw new AccessDeniedException("Chỉ Trưởng khoa được thực hiện thao tác này.");
    }
}
