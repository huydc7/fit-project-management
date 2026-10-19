package com.example.quanlydetai.dto;

public record ThongTinCaNhanResponse(
        String username,
        String code,
        String fullName,
        String email,
        String role,
        String departmentName,
        boolean dean,
        boolean departmentHead) {}
