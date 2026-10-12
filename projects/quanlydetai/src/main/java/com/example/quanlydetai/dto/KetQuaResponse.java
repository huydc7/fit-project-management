package com.example.quanlydetai.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record KetQuaResponse(
        String topic,
        String title,
        String group,
        BigDecimal score,
        String comment,
        String state,
        LocalDateTime published,
        String period,
        String periodName) {}
