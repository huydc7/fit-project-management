package com.example.quanlydetai.dto;

import java.util.List;

public record DeTaiResponse(
        String id,
        String title,
        String content,
        String department,
        String period,
        String state,
        List<String> supervisors,
        String reviewer,
        String council,
        String group,
        boolean held,
        boolean open) {}
