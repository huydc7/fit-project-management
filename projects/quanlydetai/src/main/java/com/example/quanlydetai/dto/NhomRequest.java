package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

public record NhomRequest(
        @NotBlank @Size(max = 20) String period, @NotBlank @Size(max = 150) String name) {}
