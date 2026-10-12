package com.example.quanlydetai.dto;

import java.time.LocalDateTime;

public record BaoCaoResponse(
        String id,
        String group,
        String groupName,
        String topic,
        String topicTitle,
        int version,
        String extension,
        LocalDateTime submitted) {}
