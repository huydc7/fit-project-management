package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

public record TruongBoMonRequest(
        @NotBlank @Size(max = 20) String department,
        @NotBlank @Size(max = 20) String teacher,
        @NotNull Boolean appointed) {}
