package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

public record MoiThanhVienRequest(@NotBlank @Size(max = 20) String student) {}
