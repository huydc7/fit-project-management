package com.example.quanlydetai.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.quanlydetai.entity.DotDangKy;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class AcademicRulesBoundaryTest {
    @Test
    void reviewMustFinishStrictlyBeforeStudentRegistration() {
        var rules = spy(new AcademicRules());
        var time = LocalDateTime.of(2026, 10, 19, 8, 0);
        var period = new DotDangKy();
        period.setBeginSV(time);
        doReturn(time.minusNanos(1)).when(rules).now();
        assertThatCode(() -> rules.topicReviewWindow(period)).doesNotThrowAnyException();
        doReturn(time).when(rules).now();
        assertThatThrownBy(() -> rules.topicReviewWindow(period)).hasMessageContaining("trước khi");
        doReturn(time.plusNanos(1)).when(rules).now();
        assertThatThrownBy(() -> rules.topicReviewWindow(period)).hasMessageContaining("trước khi");
    }

    @Test
    void reviewerMaySubmitAtDeadlineButNotAfterIt() {
        var rules = spy(new AcademicRules());
        var time = LocalDateTime.of(2026, 10, 19, 8, 0);
        var period = new DotDangKy();
        period.setDeadlineGvpbNopDiem(time);
        for (var now : java.util.List.of(time.minusSeconds(1), time)) {
            doReturn(now).when(rules).now();
            assertThatCode(() -> rules.reviewerDeadline(period)).doesNotThrowAnyException();
        }
        doReturn(time.plusNanos(1)).when(rules).now();
        assertThatThrownBy(() -> rules.reviewerDeadline(period)).hasMessageContaining("quá hạn");
    }
}
