package com.example.quanlydetai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "THANHVIEN_HOIDONG")
@IdClass(ThanhVienHoiDongId.class)
public class ThanhVienHoiDong extends AbstractEntity {
    @Id
    @Column(name = "Ma_HD", nullable = false, length = 20)
    private String maHd;

    @Id
    @Column(name = "Ma_GV", nullable = false, length = 20)
    private String maGV;

    @Column(name = "Vai_Tro", nullable = false, length = 20)
    private String vaiTro;

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
            name = "Ma_GV",
            referencedColumnName = "Ma_GV",
            insertable = false,
            updatable = false)
    private GiangVien giangVien;

    public GiangVien getGiangVien() {
        return giangVien;
    }

    public String getMaHd() {
        return maHd;
    }

    public void setMaHd(String maHd) {
        this.maHd = maHd;
    }

    public String getMaGV() {
        return maGV;
    }

    public void setMaGV(String maGV) {
        this.maGV = maGV;
    }

    public String getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(String vaiTro) {
        this.vaiTro = vaiTro;
    }

    @Override
    protected Object entityId() {
        return maHd == null || maGV == null ? null : new ThanhVienHoiDongId(maHd, maGV);
    }
}
