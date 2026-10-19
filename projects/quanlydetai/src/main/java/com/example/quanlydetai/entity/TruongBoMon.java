package com.example.quanlydetai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "TRUONG_BO_MON")
public class TruongBoMon extends AbstractEntity {
    @Id
    @Column(name = "Ma_Bo_Mon", length = 20)
    private String maBoMon;

    @Column(name = "Ma_GV", nullable = false, length = 20)
    private String maGV;

    @Column(name = "Dang_Giu_Chuc", nullable = false)
    private boolean dangGiuChuc = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Ma_Bo_Mon", insertable = false, updatable = false)
    private BoMon boMon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Ma_GV", insertable = false, updatable = false)
    private GiangVien giangVien;

    public String getMaBoMon() {
        return maBoMon;
    }

    public void setMaBoMon(String v) {
        maBoMon = v;
    }

    public String getMaGV() {
        return maGV;
    }

    public void setMaGV(String v) {
        maGV = v;
    }

    public boolean isDangGiuChuc() {
        return dangGiuChuc;
    }

    public void setDangGiuChuc(boolean v) {
        dangGiuChuc = v;
    }

    @Override
    protected Object entityId() {
        return maBoMon;
    }
}
