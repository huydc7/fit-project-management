package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record DeTaiRequest(
        @NotBlank @Size(max = 20) String period,
        @NotBlank @Size(max = 300) String title,
        @NotBlank @Size(max = 20000) String content,
        @NotBlank @Size(max = 20) String department,
        @NotEmpty @Size(min = 1, max = 2) List<@NotBlank @Size(max = 20) String> supervisors) {}
