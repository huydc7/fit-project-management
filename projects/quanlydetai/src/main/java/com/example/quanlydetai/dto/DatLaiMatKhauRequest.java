package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

public record DatLaiMatKhauRequest(@NotBlank @Size(min = 12, max = 64) String password) {}
