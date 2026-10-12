package com.example.quanlydetai.repository;

import com.example.quanlydetai.entity.TaiKhoan;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, String> {
    java.util.Optional<TaiKhoan> findByTenDangNhap(String tenDangNhap);
}
