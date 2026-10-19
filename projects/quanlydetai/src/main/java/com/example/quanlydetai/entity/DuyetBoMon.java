package com.example.quanlydetai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "DUYET_BO_MON")
public class DuyetBoMon extends AbstractEntity {
    @Id
    @Column(name = "Ma_De_Tai", length = 20)
    private String maDeTai;

    @Column(name = "Ma_GV", nullable = false, length = 20)
    private String maGV;

    @Column(name = "Ngay_Duyet", nullable = false)
    private LocalDateTime ngayDuyet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Ma_De_Tai", insertable = false, updatable = false)
    private DeTai deTai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Ma_GV", insertable = false, updatable = false)
    private GiangVien giangVien;

    public String getMaDeTai() {
        return maDeTai;
    }

    public void setMaDeTai(String v) {
        maDeTai = v;
    }

    public String getMaGV() {
        return maGV;
    }

    public void setMaGV(String v) {
        maGV = v;
    }

    public LocalDateTime getNgayDuyet() {
        return ngayDuyet;
    }

    public void setNgayDuyet(LocalDateTime v) {
        ngayDuyet = v;
    }

    @Override
    protected Object entityId() {
        return maDeTai;
    }
}
