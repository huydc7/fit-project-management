package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

public record BoNhiemRequest(
        @NotBlank @Size(max = 20) String teacher, @NotNull Boolean appointed) {}
