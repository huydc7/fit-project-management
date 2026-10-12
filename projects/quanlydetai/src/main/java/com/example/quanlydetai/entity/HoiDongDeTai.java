package com.example.quanlydetai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "HOI_DONG_DE_TAI")
@IdClass(HoiDongDeTaiId.class)
public class HoiDongDeTai extends AbstractEntity {
    @Id
    @Column(name = "Ma_HD", nullable = false, length = 20)
    private String maHd;

    @Id
    @Column(name = "Ma_De_Tai", nullable = false, length = 20)
    private String maDeTai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "Ma_HD",
            referencedColumnName = "Ma_HD",
            insertable = false,
            updatable = false)
    private HoiDong hd;

    public HoiDong getHd() {
        return hd;
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

    public String getMaHd() {
        return maHd;
    }

    public void setMaHd(String maHd) {
        this.maHd = maHd;
    }

    public String getMaDeTai() {
        return maDeTai;
    }

    public void setMaDeTai(String maDeTai) {
        this.maDeTai = maDeTai;
    }

    @Override
    protected Object entityId() {
        return maHd == null || maDeTai == null ? null : new HoiDongDeTaiId(maHd, maDeTai);
    }
}
