package com.example.quanlydetai.dto;

import java.time.LocalDateTime;

public record ThongBaoResponse(
        String id,
        String title,
        String content,
        String scope,
        LocalDateTime posted,
        boolean manageable) {}
