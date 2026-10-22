package com.example.quanlydetai.dto;

public record TopicSearch(String q, String department, String period, String type, String state) {
    public static TopicSearch empty() {
        return new TopicSearch("", "", "", "", "");
    }
}
