package com.example.quanlydetai.dto;

import jakarta.validation.constraints.*;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class DotDangKyForm {

    @NotBlank(message = "Mã đợt không được để trống.")
    @Size(max = 20, message = "Mã đợt tối đa 20 ký tự.")
    private String maDot;

    @NotBlank(message = "Tên đợt không được để trống.")
    @Size(max = 200, message = "Tên đợt tối đa 200 ký tự.")
    private String tenDotDangKy;

    @NotBlank(message = "Phải chọn loại đợt.")
    @Pattern(regexp = "MonHoc|NCKH|TLCN|KLTN", message = "Loại đợt không hợp lệ.")
    private String loai;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @NotNull(message = "Phải nhập thời gian bắt đầu đăng ký của giảng viên.")
    private LocalDateTime beginGV;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @NotNull(message = "Phải nhập thời gian kết thúc đăng ký của giảng viên.")
    private LocalDateTime endGV;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @NotNull(message = "Phải nhập thời gian bắt đầu đăng ký của sinh viên.")
    private LocalDateTime beginSV;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @NotNull(message = "Phải nhập thời gian kết thúc đăng ký của sinh viên.")
    private LocalDateTime endSV;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime deadlineGvpbNopDiem;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime timeBcHd;

    // getters & setters
    public String getMaDot() {
        return maDot;
    }

    public void setMaDot(String maDot) {
        this.maDot = maDot;
    }

    public String getTenDotDangKy() {
        return tenDotDangKy;
    }

    public void setTenDotDangKy(String tenDotDangKy) {
        this.tenDotDangKy = tenDotDangKy;
    }

    public String getLoai() {
        return loai;
    }

    public void setLoai(String loai) {
        this.loai = loai;
    }

    public LocalDateTime getBeginGV() {
        return beginGV;
    }

    public void setBeginGV(LocalDateTime beginGV) {
        this.beginGV = beginGV;
    }

    public LocalDateTime getEndGV() {
        return endGV;
    }

    public void setEndGV(LocalDateTime endGV) {
        this.endGV = endGV;
    }

    public LocalDateTime getBeginSV() {
        return beginSV;
    }

    public void setBeginSV(LocalDateTime beginSV) {
        this.beginSV = beginSV;
    }

    public LocalDateTime getEndSV() {
        return endSV;
    }

    public void setEndSV(LocalDateTime endSV) {
        this.endSV = endSV;
    }

    public LocalDateTime getDeadlineGvpbNopDiem() {
        return deadlineGvpbNopDiem;
    }

    public void setDeadlineGvpbNopDiem(LocalDateTime deadlineGvpbNopDiem) {
        this.deadlineGvpbNopDiem = deadlineGvpbNopDiem;
    }

    public LocalDateTime getTimeBcHd() {
        return timeBcHd;
    }

    public void setTimeBcHd(LocalDateTime timeBcHd) {
        this.timeBcHd = timeBcHd;
    }
}
