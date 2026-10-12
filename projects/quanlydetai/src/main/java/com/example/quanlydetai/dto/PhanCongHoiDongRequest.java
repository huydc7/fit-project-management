package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

public record PhanCongHoiDongRequest(@NotBlank @Size(max = 20) String council) {}
