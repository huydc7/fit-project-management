package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record DeTaiRequest(
        @NotBlank @Size(max = 20) String period,
        @NotBlank @Size(max = 300) String title,
        @NotBlank @Size(max = 20000) String content,
        @NotBlank @Size(max = 20) String department,
        @Null(message = "GVHD được tự động gán từ giảng viên đề xuất.") List<String> supervisors) {}
