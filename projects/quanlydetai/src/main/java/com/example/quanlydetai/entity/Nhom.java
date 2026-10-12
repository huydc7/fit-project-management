package com.example.quanlydetai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "NHOM")
public class Nhom extends AbstractEntity {
    @Id
    @Column(name = "Ma_Nhom", nullable = false, length = 20)
    private String maNhom;

    @Column(name = "Ten_Nhom", nullable = false, length = 150)
    private String tenNhom;

    @Column(name = "Ma_Dot", nullable = false, length = 20)
    private String maDot;

    @Column(name = "Ma_De_Tai", nullable = true, length = 20)
    private String maDeTai;

    @Column(name = "Ngay_Chon_De_Tai", nullable = true)
    private LocalDateTime ngayChonDeTai;

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
            name = "Ma_De_Tai",
            referencedColumnName = "Ma_De_Tai",
            insertable = false,
            updatable = false)
    private DeTai deTai;

    public DeTai getDeTai() {
        return deTai;
    }

    public String getMaNhom() {
        return maNhom;
    }

    public void setMaNhom(String maNhom) {
        this.maNhom = maNhom;
    }

    public String getTenNhom() {
        return tenNhom;
    }

    public void setTenNhom(String tenNhom) {
        this.tenNhom = tenNhom;
    }

    public String getMaDot() {
        return maDot;
    }

    public void setMaDot(String maDot) {
        this.maDot = maDot;
    }

    public String getMaDeTai() {
        return maDeTai;
    }

    public void setMaDeTai(String maDeTai) {
        this.maDeTai = maDeTai;
    }

    public LocalDateTime getNgayChonDeTai() {
        return ngayChonDeTai;
    }

    public void setNgayChonDeTai(LocalDateTime ngayChonDeTai) {
        this.ngayChonDeTai = ngayChonDeTai;
    }

    @Override
    protected Object entityId() {
        return maNhom;
    }
}
