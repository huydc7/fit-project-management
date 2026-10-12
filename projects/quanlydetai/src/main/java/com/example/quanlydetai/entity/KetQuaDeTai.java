package com.example.quanlydetai.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "KET_QUA_DE_TAI")
public class KetQuaDeTai extends AbstractEntity {
    @Id
    @Column(name = "Ma_De_Tai", nullable = false, length = 20)
    private String maDeTai;

    @Column(name = "Diem_Cuoi", nullable = false, precision = 5, scale = 2)
    private BigDecimal diemCuoi;

    @Column(name = "Nhan_Xet_Tong_Hop", nullable = true, columnDefinition = "TEXT")
    private String nhanXetTongHop;

    @Column(name = "Ket_Qua", nullable = true, length = 100)
    private String ketQua;

    @Column(name = "Ma_GV_Tong_Hop", nullable = false, length = 20)
    private String maGvTongHop;

    @Column(name = "Trang_Thai_Cong_Bo", nullable = false, length = 20)
    private String trangThaiCongBo;

    @Column(name = "Ngay_Tong_Hop", nullable = false)
    private LocalDateTime ngayTongHop;

    @Column(name = "Ngay_Cong_Bo", nullable = true)
    private LocalDateTime ngayCongBo;

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
            name = "Ma_GV_Tong_Hop",
            referencedColumnName = "Ma_GV",
            insertable = false,
            updatable = false)
    private GiangVien gvTongHop;

    public GiangVien getGvTongHop() {
        return gvTongHop;
    }

    public String getMaDeTai() {
        return maDeTai;
    }

    public void setMaDeTai(String maDeTai) {
        this.maDeTai = maDeTai;
    }

    public BigDecimal getDiemCuoi() {
        return diemCuoi;
    }

    public void setDiemCuoi(BigDecimal diemCuoi) {
        this.diemCuoi = diemCuoi;
    }

    public String getNhanXetTongHop() {
        return nhanXetTongHop;
    }

    public void setNhanXetTongHop(String nhanXetTongHop) {
        this.nhanXetTongHop = nhanXetTongHop;
    }

    public String getKetQua() {
        return ketQua;
    }

    public void setKetQua(String ketQua) {
        this.ketQua = ketQua;
    }

    public String getMaGvTongHop() {
        return maGvTongHop;
    }

    public void setMaGvTongHop(String maGvTongHop) {
        this.maGvTongHop = maGvTongHop;
    }

    public String getTrangThaiCongBo() {
        return trangThaiCongBo;
    }

    public void setTrangThaiCongBo(String trangThaiCongBo) {
        this.trangThaiCongBo = trangThaiCongBo;
    }

    public LocalDateTime getNgayTongHop() {
        return ngayTongHop;
    }

    public void setNgayTongHop(LocalDateTime ngayTongHop) {
        this.ngayTongHop = ngayTongHop;
    }

    public LocalDateTime getNgayCongBo() {
        return ngayCongBo;
    }

    public void setNgayCongBo(LocalDateTime ngayCongBo) {
        this.ngayCongBo = ngayCongBo;
    }

    @Override
    protected Object entityId() {
        return maDeTai;
    }
}
