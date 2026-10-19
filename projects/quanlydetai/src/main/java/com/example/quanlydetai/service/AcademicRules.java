package com.example.quanlydetai.service;

import com.example.quanlydetai.entity.DotDangKy;
import com.example.quanlydetai.exception.BusinessException;

import jakarta.persistence.*;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/** Shared registration windows, Vietnam clock and invariant checks. */
@Component
public class AcademicRules {
    public static final int MIN_GROUP_MEMBERS = 3;
    public static final int MAX_GROUP_MEMBERS = 5;

    public void registrationGroupSize(int size) {
        check(
                size >= MIN_GROUP_MEMBERS && size <= MAX_GROUP_MEMBERS,
                "Nhóm phải có từ 3 đến 5 thành viên đã xác nhận để đăng ký đề tài.");
    }

    public void groupHasSpace(int size) {
        check(size < MAX_GROUP_MEMBERS, "Nhóm đã đủ 5 thành viên.");
    }

    public String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public void check(boolean condition, String message) {
        if (!condition) throw new BusinessException(message);
    }

    public void studentWindow(DotDangKy d) {
        window(d.getBeginSV(), d.getEndSV(), "Đã ngoài thời gian sinh viên đăng ký.");
    }

    public void teacherWindow(DotDangKy d) {
        var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        window(
                d.getBeginGV(),
                d.getEndGV(),
                "Đợt chỉ nhận đề xuất từ "
                        + d.getBeginGV().format(fmt)
                        + " đến "
                        + d.getEndGV().format(fmt)
                        + " (giờ Việt Nam).");
    }

    public LocalDateTime now() {
        return LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    public void topicReviewWindow(DotDangKy d) {
        check(
                now().isBefore(d.getBeginSV()),
                "Chỉ duyệt/công bố đề tài trước khi bắt đầu đăng ký sinh viên.");
    }

    public void reviewerDeadline(DotDangKy d) {
        check(
                d.getDeadlineGvpbNopDiem() == null || !now().isAfter(d.getDeadlineGvpbNopDiem()),
                "Đã quá hạn nộp điểm phản biện.");
    }

    public boolean inWindow(LocalDateTime begin, LocalDateTime end) {
        var now = now();
        return !now.isBefore(begin) && !now.isAfter(end);
    }

    private void window(LocalDateTime begin, LocalDateTime end, String message) {
        check(inWindow(begin, end), message);
    }
}
