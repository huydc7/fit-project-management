package com.example.quanlydetai.dto;

/** A valid group choice for one operation, independent of history pagination. */
public record NhomOption(
        String id, String name, String periodId, String period, String topic, String topicTitle) {}
