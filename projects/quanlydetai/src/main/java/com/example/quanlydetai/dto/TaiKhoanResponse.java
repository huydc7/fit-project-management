package com.example.quanlydetai.dto;

public record TaiKhoanResponse(
        String id,
        String username,
        String role,
        String profile,
        String state,
        String name,
        String email,
        String department,
        boolean dean,
        boolean departmentHead) {
    public String roleLabel() {
        return switch (role) {
            case "Admin" -> "Quản trị viên";
            case "GV" -> "Giảng viên";
            case "SV" -> "Sinh viên";
            default -> role;
        };
    }
}
