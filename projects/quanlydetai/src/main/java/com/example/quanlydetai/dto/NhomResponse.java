package com.example.quanlydetai.dto;

import java.util.List;

public record NhomResponse(
        String id,
        String name,
        String period,
        String topic,
        String topicTitle,
        boolean leader,
        boolean editable,
        List<ThanhVienResponse> members,
        List<DangKyDeTaiResponse> registrations) {}
