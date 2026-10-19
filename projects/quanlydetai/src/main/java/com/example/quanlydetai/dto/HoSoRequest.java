package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

public record HoSoRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 20) String department) {}
