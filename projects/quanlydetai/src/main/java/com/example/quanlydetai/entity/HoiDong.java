package com.example.quanlydetai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "HOI_DONG")
public class HoiDong extends AbstractEntity {
    @Id
    @Column(name = "Ma_HD", nullable = false, length = 20)
    private String maHd;

    @Column(name = "Ma_Dot", nullable = false, length = 20)
    private String maDot;

    @Column(name = "Trang_Thai", nullable = false, length = 20)
    private String trangThai;

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

    public String getMaHd() {
        return maHd;
    }

    public void setMaHd(String maHd) {
        this.maHd = maHd;
    }

    public String getMaDot() {
        return maDot;
    }

    public void setMaDot(String maDot) {
        this.maDot = maDot;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    @Override
    protected Object entityId() {
        return maHd;
    }
}
