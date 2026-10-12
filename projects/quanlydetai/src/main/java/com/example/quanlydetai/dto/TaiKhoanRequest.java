package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

public record TaiKhoanRequest(
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,100}") String username,
        @NotBlank @Size(min = 12, max = 64) String password,
        @NotBlank @Pattern(regexp = "SV|GV|Admin") String role,
        @Size(max = 20) String profileId,
        @Size(max = 150) String name,
        @Email @Size(max = 255) String email,
        @Size(max = 20) String department) {}
