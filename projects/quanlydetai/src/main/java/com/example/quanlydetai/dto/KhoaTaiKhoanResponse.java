package com.example.quanlydetai.dto;

import java.util.List;

public record KhoaTaiKhoanResponse(
        String accountId, String username, String name, String state, List<Task> tasks) {
    public record Task(
            String role, String topicId, String topicTitle, String councilId, String guidance) {}
}
