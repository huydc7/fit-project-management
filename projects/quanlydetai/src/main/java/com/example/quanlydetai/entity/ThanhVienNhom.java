package com.example.quanlydetai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "THANH_VIEN_NHOM")
@IdClass(ThanhVienNhomId.class)
public class ThanhVienNhom extends AbstractEntity {
    @Id
    @Column(name = "Ma_Nhom", nullable = false, length = 20)
    private String maNhom;

    @Id
    @Column(name = "Ma_SV", nullable = false, length = 20)
    private String maSV;

    @Column(name = "Vai_Tro", nullable = false, length = 30)
    private String vaiTro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "Ma_Nhom",
            referencedColumnName = "Ma_Nhom",
            insertable = false,
            updatable = false)
    private Nhom nhom;

    public Nhom getNhom() {
        return nhom;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "Ma_SV",
            referencedColumnName = "Ma_SV",
            insertable = false,
            updatable = false)
    private SinhVien sinhVien;

    public SinhVien getSinhVien() {
        return sinhVien;
    }

    public String getMaNhom() {
        return maNhom;
    }

    public void setMaNhom(String maNhom) {
        this.maNhom = maNhom;
    }

    public String getMaSV() {
        return maSV;
    }

    public void setMaSV(String maSV) {
        this.maSV = maSV;
    }

    public String getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(String vaiTro) {
        this.vaiTro = vaiTro;
    }

    @Override
    protected Object entityId() {
        return maNhom == null || maSV == null ? null : new ThanhVienNhomId(maNhom, maSV);
    }
}
