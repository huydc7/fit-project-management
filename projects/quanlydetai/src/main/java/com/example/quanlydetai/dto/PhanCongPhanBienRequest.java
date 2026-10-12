package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

public record PhanCongPhanBienRequest(@NotBlank @Size(max = 20) String teacher) {}
