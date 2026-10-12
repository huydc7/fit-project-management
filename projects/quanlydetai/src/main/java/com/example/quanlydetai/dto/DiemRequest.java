package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record DiemRequest(
        @NotBlank @Size(max = 20) String topic,
        @Pattern(regexp = "GVPB|HD") @NotBlank String type,
        @NotNull @DecimalMin("0.00") @DecimalMax("10.00") @Digits(integer = 2, fraction = 2)
                BigDecimal value,
        @NotBlank @Size(max = 5000) String comment) {}
