package com.example.quanlydetai.repository;

import com.example.quanlydetai.entity.ThanhVienHoiDong;
import com.example.quanlydetai.entity.ThanhVienHoiDongId;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ThanhVienHoiDongRepository
        extends JpaRepository<ThanhVienHoiDong, ThanhVienHoiDongId> {}
