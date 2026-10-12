package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

public record TongHopDiemRequest(@NotBlank @Size(max = 5000) String comment) {}
