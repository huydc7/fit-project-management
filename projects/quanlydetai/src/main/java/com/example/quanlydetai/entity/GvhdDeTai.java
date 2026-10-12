package com.example.quanlydetai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "GVHD_DETAI")
@IdClass(GvhdDeTaiId.class)
public class GvhdDeTai extends AbstractEntity {
    @Id
    @Column(name = "Ma_GV", nullable = false, length = 20)
    private String maGV;

    @Id
    @Column(name = "Ma_De_Tai", nullable = false, length = 20)
    private String maDeTai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "Ma_GV",
            referencedColumnName = "Ma_GV",
            insertable = false,
            updatable = false)
    private GiangVien giangVien;

    public GiangVien getGiangVien() {
        return giangVien;
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

    public String getMaGV() {
        return maGV;
    }

    public void setMaGV(String maGV) {
        this.maGV = maGV;
    }

    public String getMaDeTai() {
        return maDeTai;
    }

    public void setMaDeTai(String maDeTai) {
        this.maDeTai = maDeTai;
    }

    @Override
    protected Object entityId() {
        return maGV == null || maDeTai == null ? null : new GvhdDeTaiId(maGV, maDeTai);
    }
}
