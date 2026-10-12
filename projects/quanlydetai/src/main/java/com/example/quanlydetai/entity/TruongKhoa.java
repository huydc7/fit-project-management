package com.example.quanlydetai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "TRUONG_KHOA")
public class TruongKhoa extends AbstractEntity {
    @Column(name = "Dang_Giu_Chuc", nullable = false)
    private boolean dangGiuChuc = true;

    public boolean isDangGiuChuc() {
        return dangGiuChuc;
    }

    public void setDangGiuChuc(boolean value) {
        dangGiuChuc = value;
    }

    @Id
    @Column(name = "Ma_GV", nullable = false, length = 20)
    private String maGV;

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

    public String getMaGV() {
        return maGV;
    }

    public void setMaGV(String maGV) {
        this.maGV = maGV;
    }

    @Override
    protected Object entityId() {
        return maGV;
    }
}
