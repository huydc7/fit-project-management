package com.example.quanlydetai.dto;

import java.time.LocalDateTime;

public record DangKyDeTaiResponse(
        String id,
        String group,
        String groupName,
        String topic,
        String topicTitle,
        String status,
        LocalDateTime submitted,
        String reason) {}
