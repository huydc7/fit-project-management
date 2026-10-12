package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

public record DangKyDeTaiRequest(
        @NotBlank @Size(max = 20) String group, @NotBlank @Size(max = 20) String topic) {}
