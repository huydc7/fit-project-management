package com.example.quanlydetai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CapNhatNhomRequest(
        @NotBlank(message = "Phải nhập tên nhóm.")
                @Size(max = 150, message = "Tên nhóm không được vượt quá 150 ký tự.")
                String name) {}
