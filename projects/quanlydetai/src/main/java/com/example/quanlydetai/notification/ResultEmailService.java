package com.example.quanlydetai.notification;

import com.example.quanlydetai.entity.*;
import com.example.quanlydetai.repository.JpaEntityRepository;
import com.example.quanlydetai.repository.ResultEmailRepository;
import com.example.quanlydetai.service.AcademicRules;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ResultEmailService {
    private static final int MAX_ATTEMPTS = 5;
    private final ResultEmailRepository emails;
    private final JpaEntityRepository entities;
    private final AcademicRules rules;
    private final String siteUrl;

    public ResultEmailService(
            ResultEmailRepository emails,
            JpaEntityRepository entities,
            AcademicRules rules,
            @Value("${app.public-url:http://localhost:8080}") String siteUrl) {
        this.emails = emails;
        this.entities = entities;
        this.rules = rules;
        this.siteUrl = siteUrl.replaceAll("/+$", "");
    }

    /** Same transaction as publication: rollback also removes every queued email. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(DeTai topic, KetQuaDeTai result) {
        var students =
                entities.query(
                        "select s from SinhVien s where s.maSV in (select m.maSV from ThanhVienNhom"
                                + " m where m.maNhom in (select n.maNhom from Nhom n where"
                                + " n.maDeTai=?1)) order by s.maSV",
                        SinhVien.class,
                        topic.getMaDeTai());
        rules.check(!students.isEmpty(), "Đề tài chưa có thành viên nhận kết quả.");
        var period = entities.require(DotDangKy.class, topic.getMaDot());
        for (var student : students) {
            var mail = new ResultEmail();
            mail.setId(UUID.randomUUID().toString().replace("-", ""));
            mail.setTopic(topic.getMaDeTai());
            mail.setStudent(student.getMaSV());
            mail.setRecipient(student.getEmail());
            mail.setSubject("FIT - Kết quả đề tài đã được công bố");
            mail.setBody(
                    "Chào "
                            + student.getHoTen()
                            + ",\n\nKết quả đề tài của bạn đã được công bố.\n"
                            + "Đề tài: "
                            + topic.getTenDeTai()
                            + "\nĐợt: "
                            + period.getTenDotDangKy()
                            + "\nĐiểm: "
                            + result.getDiemCuoi().toPlainString()
                            + "/10\n\n"
                            + "Đăng nhập để xem chi tiết: "
                            + siteUrl
                            + "/student/results?period="
                            + java.net.URLEncoder.encode(
                                    topic.getMaDot(), java.nio.charset.StandardCharsets.UTF_8)
                            + "\n\nKhoa Công nghệ thông tin - Hệ thống quản lý đề tài sinh viên");
            mail.setState("PENDING");
            mail.setCreatedAt(rules.now());
            // SQL DATETIME has second precision; rounding up must not delay an immediate claim.
            mail.setNextAttemptAt(rules.now().withNano(0));
            emails.save(mail);
        }
        emails.flush();
    }

    public record Delivery(String id, String recipient, String subject, String body, int attempt) {}

    public record Status(
            String id,
            String topic,
            String student,
            String recipient,
            String state,
            int attempts,
            LocalDateTime created,
            LocalDateTime accepted) {}

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Delivery claim() {
        var now = rules.now();
        // Retire expired final attempts first, so they cannot hide other due recipients.
        // Active leases are untouched. This update and the following claim share one transaction.
        emails.failExhausted(MAX_ATTEMPTS, now);
        var row =
                emails.findFirstByStateInAndNextAttemptAtLessThanEqualOrderByCreatedAtAscIdAsc(
                                List.of("PENDING", "PROCESSING"), now)
                        .orElse(null);
        if (row == null) return null;
        row.setState("PROCESSING");
        row.setAttempts(row.getAttempts() + 1);
        // A crashed process can be retried after the lease. Never assume exactly-once SMTP.
        row.setNextAttemptAt(rules.now().plusMinutes(2));
        return new Delivery(
                row.getId(),
                row.getRecipient(),
                row.getSubject(),
                row.getBody(),
                row.getAttempts());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String id, int attempt, String receipt) {
        var row = emails.lockById(id).orElseThrow();
        if (!"PROCESSING".equals(row.getState()) || row.getAttempts() != attempt) return;
        if (receipt != null) {
            row.setState("ACCEPTED");
            row.setAcceptedAt(rules.now());
            row.setReceipt(receipt.substring(0, Math.min(receipt.length(), 512)));
        } else {
            row.setState(row.getAttempts() >= MAX_ATTEMPTS ? "FAILED" : "PENDING");
            row.setNextAttemptAt(rules.now().plusMinutes(1L << (row.getAttempts() - 1)));
        }
    }

    @PreAuthorize("hasRole('TRUONGKHOA')")
    @Transactional(readOnly = true)
    public StatusPage search(String q, String state, int page, int size) {
        String keyword = q == null ? "" : q.strip().toLowerCase(Locale.ROOT);
        String selectedState = state == null ? "" : state.strip().toUpperCase(Locale.ROOT);
        rules.check(keyword.length() <= 200, "Từ khóa tối đa 200 ký tự.");
        rules.check(
                selectedState.isEmpty()
                        || Set.of("PENDING", "PROCESSING", "ACCEPTED", "FAILED")
                                .contains(selectedState),
                "Trạng thái email không hợp lệ.");
        rules.check(
                page >= 0 && page <= 1_000_000 && size >= 1 && size <= 100,
                "Trang phải từ 0 đến 1000000; số dòng mỗi trang từ 1 đến 100.");
        // Search treats %, _ and ! literally rather than allowing wildcard expansion.
        String pattern =
                "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        var rows =
                emails.search(
                        selectedState,
                        pattern,
                        PageRequest.of(
                                page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        var content =
                rows.stream()
                        .map(
                                e ->
                                        new Status(
                                                e.getId(),
                                                e.getTopic(),
                                                e.getStudent(),
                                                e.getRecipient(),
                                                e.getState(),
                                                e.getAttempts(),
                                                e.getCreatedAt(),
                                                e.getAcceptedAt()))
                        .toList();
        return new StatusPage(content, page, size, rows.getTotalElements(), rows.getTotalPages());
    }

    public record StatusPage(
            List<Status> content, int page, int size, long totalElements, int totalPages) {}

    @PreAuthorize("hasRole('TRUONGKHOA')")
    @Transactional
    public void retry(String id) {
        var row =
                emails.lockById(id)
                        .orElseThrow(
                                () ->
                                        new com.example.quanlydetai.exception.BusinessException(
                                                "Không tìm thấy email."));
        rules.check("FAILED".equals(row.getState()), "Chỉ gửi lại email đã thất bại.");
        row.setAttempts(0);
        row.setState("PENDING");
        row.setNextAttemptAt(rules.now().withNano(0));
    }
}
