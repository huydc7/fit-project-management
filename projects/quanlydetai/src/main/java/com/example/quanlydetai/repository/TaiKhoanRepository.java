package com.example.quanlydetai.repository;

import com.example.quanlydetai.entity.TaiKhoan;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, String> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"giangVien", "sinhVien"})
    java.util.Optional<TaiKhoan> findByTenDangNhap(String tenDangNhap);

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"giangVien", "sinhVien"})
    java.util.Optional<TaiKhoan> findById(String id);
}
