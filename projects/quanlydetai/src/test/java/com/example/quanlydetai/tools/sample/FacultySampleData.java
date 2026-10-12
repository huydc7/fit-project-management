package com.example.quanlydetai.tools.sample;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/** File format for optional local sample data. No passwords or real university personnel. */
public record FacultySampleData(
        @NotBlank String description,
        @NotEmpty List<@Valid Department> departments,
        @NotEmpty List<@Valid Teacher> teachers,
        @NotEmpty List<@Valid Student> students,
        @Valid @NotNull Administrator administrator,
        @NotEmpty List<@Valid Period> periods,
        @NotEmpty List<@Valid Topic> topics,
        @Valid @NotNull Notice notice) {

    public record Department(
            @NotBlank @Size(max = 20) String id, @NotBlank @Size(max = 150) String name) {}

    public record Teacher(
            @NotBlank @Size(max = 20) String id,
            String legacyId,
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Email String email,
            @NotBlank String department,
            boolean dean) {}

    public record Student(
            @NotBlank @Size(max = 20) String id,
            String legacyId,
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Email String email) {}

    public record Administrator(
            @NotBlank @Size(max = 20) String id,
            String legacyId,
            @NotBlank @Size(max = 100) String username) {}

    public record Period(
            @NotBlank @Size(max = 20) String id,
            String legacyId,
            @NotBlank @Size(max = 200) String name,
            @Pattern(regexp = "MonHoc|NCKH|TLCN|KLTN") @NotBlank String type,
            int teacherStartDays,
            int teacherEndDays,
            int studentStartDays,
            int studentEndDays,
            Integer reviewDeadlineDays,
            Integer councilDays) {}

    public record Topic(
            @NotBlank @Size(max = 20) String id,
            String legacyId,
            @NotBlank @Size(max = 300) String title,
            @NotBlank @Size(max = 20000) String content,
            @NotBlank String department,
            @NotBlank String period,
            @NotEmpty @Size(max = 2) List<@NotBlank String> supervisors,
            boolean published) {}

    public record Notice(
            @NotBlank @Size(max = 20) String id,
            String legacyId,
            @NotBlank @Size(max = 300) String title,
            @NotBlank @Size(max = 20000) String content) {}
}
