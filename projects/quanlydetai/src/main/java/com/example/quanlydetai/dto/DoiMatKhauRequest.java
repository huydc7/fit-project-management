package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

public record DoiMatKhauRequest(
        @NotBlank @Size(max = 100) String currentPassword,
        @NotBlank @Size(min = 12, max = 64) String newPassword,
        @NotBlank @Size(max = 64) String confirmation) {}
