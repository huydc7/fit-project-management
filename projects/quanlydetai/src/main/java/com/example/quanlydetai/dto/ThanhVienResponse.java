package com.example.quanlydetai.dto;

public record ThanhVienResponse(String id, String name, String role) {
    public String roleLabel() {
        return switch (role) {
            case "Truong_Nhom" -> "Nhóm trưởng";
            case "Thanh_Vien" -> "Thành viên";
            case "ChuTich" -> "Chủ tịch hội đồng";
            case "ThuKy" -> "Thư ký hội đồng";
            case "ThanhVien" -> "Ủy viên hội đồng";
            default -> role;
        };
    }
}
