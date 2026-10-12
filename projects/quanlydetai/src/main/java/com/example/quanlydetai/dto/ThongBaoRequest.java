package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

public record ThongBaoRequest(
        @NotBlank @Size(max = 300) String title,
        @NotBlank @Size(max = 20000) String content,
        @NotBlank @Pattern(regexp = "Khoa|NhaTruong") String scope) {}
