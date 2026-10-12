package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record HoiDongRequest(
        @NotBlank @Size(max = 20) String period,
        @NotBlank @Size(max = 20) String chair,
        @NotBlank @Size(max = 20) String secretary,
        @NotEmpty @Size(min = 1, max = 3) List<@NotBlank @Size(max = 20) String> members) {}
