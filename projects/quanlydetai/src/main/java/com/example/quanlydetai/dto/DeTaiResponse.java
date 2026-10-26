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
        boolean open,
        String periodId,
        String departmentId,
        boolean editable,
        boolean departmentApproved,
        List<String> supervisorIds,
        String periodType,
        boolean reviewOpen,
        String proposerId,
        String confirmedBy,
        java.time.LocalDateTime confirmedAt,
        String proposerName,
        String groupName,
        List<ThanhVienResponse> groupMembers,
        List<ThanhVienResponse> councilMembers) {}
