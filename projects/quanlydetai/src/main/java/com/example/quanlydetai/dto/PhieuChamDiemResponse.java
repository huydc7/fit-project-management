package com.example.quanlydetai.dto;

import java.math.BigDecimal;
import java.util.List;

public record PhieuChamDiemResponse(
        String topic,
        String title,
        String type,
        BigDecimal score,
        String comment,
        boolean locked,
        boolean assignmentsComplete,
        boolean chair,
        int submitted,
        int required,
        List<BaoCaoResponse> reports) {}
