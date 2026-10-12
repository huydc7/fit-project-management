package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

public record DuyetRequest(@NotNull Boolean accept, @Size(max = 5000) String reason) {}
