package com.example.quanlydetai.dto;

import java.time.LocalDateTime;

public record DotDangKyView(
        String maDot,
        String tenDotDangKy,
        String loai,
        LocalDateTime beginGV,
        LocalDateTime endGV,
        LocalDateTime beginSV,
        LocalDateTime endSV) {}
