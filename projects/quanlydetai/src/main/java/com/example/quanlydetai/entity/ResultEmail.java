package com.example.quanlydetai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "EMAIL_KET_QUA",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "UQ_EMAIL_KQ_SV",
                        columnNames = {"Ma_De_Tai", "Ma_SV"}))
public class ResultEmail {
    @Id
    @Column(name = "Ma_Email", nullable = false, length = 32)
    private String id;

    @Column(name = "Ma_De_Tai", nullable = false, length = 20)
    private String topic;

    @Column(name = "Ma_SV", nullable = false, length = 20)
    private String student;

    @Column(name = "Nguoi_Nhan", nullable = false, length = 255)
    private String recipient;

    @Column(name = "Tieu_De", nullable = false, length = 255)
    private String subject;

    @Column(name = "Noi_Dung", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "Trang_Thai", nullable = false, length = 20)
    private String state;

    @Column(name = "So_Lan_Thu", nullable = false)
    private int attempts;

    @Column(name = "Ngay_Tao", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "Lan_Thu_Tiep", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "Ngay_Chap_Nhan", nullable = true)
    private LocalDateTime acceptedAt;

    @Column(name = "Ma_Dich_Vu", nullable = true, length = 512)
    private String receipt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getStudent() {
        return student;
    }

    public void setStudent(String student) {
        this.student = student;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(LocalDateTime nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }
}
