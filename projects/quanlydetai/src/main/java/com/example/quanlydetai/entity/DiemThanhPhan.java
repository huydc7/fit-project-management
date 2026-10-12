package com.example.quanlydetai.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "DIEM_THANH_PHAN")
public class DiemThanhPhan extends AbstractEntity {
    @Id
    @Column(name = "Ma_Diem", nullable = false, length = 30)
    private String maDiem;

    @Column(name = "Ma_GV", nullable = false, length = 20)
    private String maGV;

    @Column(name = "Ma_De_Tai", nullable = false, length = 20)
    private String maDeTai;

    @Column(name = "Ma_Loai_Diem", nullable = false, length = 20)
    private String maLoaiDiem;

    @Column(name = "Diem", nullable = false, precision = 5, scale = 2)
    private BigDecimal diem;

    @Column(name = "Ngay_Cham", nullable = false)
    private LocalDateTime ngayCham;

    @Column(name = "Ngay_Nop_Diem", nullable = false)
    private LocalDateTime ngayNopDiem;

    @Column(name = "Nhan_Xet", nullable = true, columnDefinition = "TEXT")
    private String nhanXet;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "Ma_Loai_Diem",
            referencedColumnName = "Ma_Loai_Diem",
            insertable = false,
            updatable = false)
    private LoaiDiem loaiDiem;

    public LoaiDiem getLoaiDiem() {
        return loaiDiem;
    }

    public String getMaDiem() {
        return maDiem;
    }

    public void setMaDiem(String maDiem) {
        this.maDiem = maDiem;
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

    public String getMaLoaiDiem() {
        return maLoaiDiem;
    }

    public void setMaLoaiDiem(String maLoaiDiem) {
        this.maLoaiDiem = maLoaiDiem;
    }

    public BigDecimal getDiem() {
        return diem;
    }

    public void setDiem(BigDecimal diem) {
        this.diem = diem;
    }

    public LocalDateTime getNgayCham() {
        return ngayCham;
    }

    public void setNgayCham(LocalDateTime ngayCham) {
        this.ngayCham = ngayCham;
    }

    public LocalDateTime getNgayNopDiem() {
        return ngayNopDiem;
    }

    public void setNgayNopDiem(LocalDateTime ngayNopDiem) {
        this.ngayNopDiem = ngayNopDiem;
    }

    public String getNhanXet() {
        return nhanXet;
    }

    public void setNhanXet(String nhanXet) {
        this.nhanXet = nhanXet;
    }

    @Override
    protected Object entityId() {
        return maDiem;
    }
}
