package com.example.quanlydetai.dto;

import java.util.List;

public record HoiDongResponse(
        String id,
        String period,
        String state,
        List<ThanhVienResponse> members,
        List<String> topics) {}
