package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

public record BoMonRequest(
        @NotBlank @Size(max = 20) String id, @NotBlank @Size(max = 150) String name) {}
