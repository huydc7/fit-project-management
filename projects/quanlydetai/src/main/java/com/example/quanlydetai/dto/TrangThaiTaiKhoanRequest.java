package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

public record TrangThaiTaiKhoanRequest(@NotNull Boolean active, Boolean confirmPendingTasks) {
    public TrangThaiTaiKhoanRequest(Boolean active) {
        this(active, false);
    }
}
