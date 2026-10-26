package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record DepartmentReviewRequest(
        @NotNull Boolean accept,
        @Null(message = "GVHD duy nhất là người đề xuất; không được chọn thêm hoặc đổi GVHD.") List<String> supervisors) {}
