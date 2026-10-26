package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record NoiDungDeTaiRequest(
        @NotBlank @Size(max = 300) String title,
        @NotBlank @Size(max = 20000) String content,
        @Size(min = 1, max = 20) String department,
        @Null(message = "Người đề xuất là GVHD duy nhất; không được chọn thêm hoặc đổi GVHD.") List<String> supervisors) {
    public NoiDungDeTaiRequest(String title, String content) {
        this(title, content, null, null);
    }
}
