package com.example.quanlydetai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "DOT_DANG_KY")
public class DotDangKy extends AbstractEntity {
    @Id
    @Column(name = "Ma_Dot", nullable = false, length = 20)
    private String maDot;

    @Column(name = "Ma_Truong_Khoa", nullable = false, length = 20)
    private String maTruongKhoa;

    @Column(name = "Ten_Dot_Dang_Ky", nullable = false, length = 200)
    private String tenDotDangKy;

    @Column(name = "Loai", nullable = false, length = 20)
    private String loai;

    @Column(name = "Begin_GV", nullable = false)
    private LocalDateTime beginGV;

    @Column(name = "End_GV", nullable = false)
    private LocalDateTime endGV;

    @Column(name = "Begin_SV", nullable = false)
    private LocalDateTime beginSV;

    @Column(name = "End_SV", nullable = false)
    private LocalDateTime endSV;

    @Column(name = "Deadline_GVPB_Nop_Diem", nullable = true)
    private LocalDateTime deadlineGvpbNopDiem;

    @Column(name = "Time_BC_HD", nullable = true)
    private LocalDateTime timeBcHd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "Ma_Truong_Khoa",
            referencedColumnName = "Ma_GV",
            insertable = false,
            updatable = false)
    private TruongKhoa truongKhoa;

    public TruongKhoa getTruongKhoa() {
        return truongKhoa;
    }

    public String getMaDot() {
        return maDot;
    }

    public void setMaDot(String maDot) {
        this.maDot = maDot;
    }

    public String getMaTruongKhoa() {
        return maTruongKhoa;
    }

    public void setMaTruongKhoa(String maTruongKhoa) {
        this.maTruongKhoa = maTruongKhoa;
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

    @Override
    protected Object entityId() {
        return maDot;
    }
}
