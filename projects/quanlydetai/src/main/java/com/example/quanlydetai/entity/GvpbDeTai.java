package com.example.quanlydetai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "GVPB_DETAI")
@IdClass(GvpbDeTaiId.class)
public class GvpbDeTai extends AbstractEntity {
    @Id
    @Column(name = "Ma_GV", nullable = false, length = 20)
    private String maGV;

    @Id
    @Column(name = "Ma_De_Tai", nullable = false, length = 20)
    private String maDeTai;

    @Column(name = "Ngay_Phan_Cong", nullable = false)
    private LocalDateTime ngayPhanCong;

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

    public LocalDateTime getNgayPhanCong() {
        return ngayPhanCong;
    }

    public void setNgayPhanCong(LocalDateTime ngayPhanCong) {
        this.ngayPhanCong = ngayPhanCong;
    }

    @Override
    protected Object entityId() {
        return maGV == null || maDeTai == null ? null : new GvpbDeTaiId(maGV, maDeTai);
    }
}
