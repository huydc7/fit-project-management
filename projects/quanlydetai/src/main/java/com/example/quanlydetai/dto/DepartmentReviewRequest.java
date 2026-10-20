package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record DepartmentReviewRequest(
        @NotNull Boolean accept,
        List<@NotBlank @Size(max = 20) String> supervisors) {}
