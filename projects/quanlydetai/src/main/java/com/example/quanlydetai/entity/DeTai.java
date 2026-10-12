package com.example.quanlydetai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "DE_TAI")
public class DeTai extends AbstractEntity {
    @Id
    @Column(name = "Ma_De_Tai", nullable = false, length = 20)
    private String maDeTai;

    @Column(name = "Ten_De_Tai", nullable = false, length = 300)
    private String tenDeTai;

    @Column(name = "Ma_Bo_Mon", nullable = false, length = 20)
    private String maBoMon;

    @Column(name = "Noi_Dung", nullable = false, columnDefinition = "TEXT")
    private String noiDung;

    @Column(name = "Trang_Thai", nullable = false, length = 30)
    private String trangThai;

    @Column(name = "Ma_Dot", nullable = false, length = 20)
    private String maDot;

    @Column(name = "Ma_GV_Dang_Ky", nullable = false, length = 20)
    private String maGvDangKy;

    @Column(name = "Ngay_Dang_Ky", nullable = false)
    private LocalDateTime ngayDangKy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "Ma_Bo_Mon",
            referencedColumnName = "Ma_Bo_Mon",
            insertable = false,
            updatable = false)
    private BoMon boMon;

    public BoMon getBoMon() {
        return boMon;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "Ma_Dot",
            referencedColumnName = "Ma_Dot",
            insertable = false,
            updatable = false)
    private DotDangKy dot;

    public DotDangKy getDot() {
        return dot;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "Ma_GV_Dang_Ky",
            referencedColumnName = "Ma_GV",
            insertable = false,
            updatable = false)
    private GiangVien gvDangKy;

    public GiangVien getGvDangKy() {
        return gvDangKy;
    }

    public String getMaDeTai() {
        return maDeTai;
    }

    public void setMaDeTai(String maDeTai) {
        this.maDeTai = maDeTai;
    }

    public String getTenDeTai() {
        return tenDeTai;
    }

    public void setTenDeTai(String tenDeTai) {
        this.tenDeTai = tenDeTai;
    }

    public String getMaBoMon() {
        return maBoMon;
    }

    public void setMaBoMon(String maBoMon) {
        this.maBoMon = maBoMon;
    }

    public String getNoiDung() {
        return noiDung;
    }

    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public String getMaDot() {
        return maDot;
    }

    public void setMaDot(String maDot) {
        this.maDot = maDot;
    }

    public String getMaGvDangKy() {
        return maGvDangKy;
    }

    public void setMaGvDangKy(String maGvDangKy) {
        this.maGvDangKy = maGvDangKy;
    }

    public LocalDateTime getNgayDangKy() {
        return ngayDangKy;
    }

    public void setNgayDangKy(LocalDateTime ngayDangKy) {
        this.ngayDangKy = ngayDangKy;
    }

    @Override
    protected Object entityId() {
        return maDeTai;
    }
}
