package com.example.quanlydetai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "LOI_MOI_NHOM",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"Ma_Nhom", "Ma_SV"})})
public class LoiMoiNhom extends AbstractEntity {
    @Id
    @Column(name = "Ma_Loi_Moi", nullable = false, length = 30)
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String value) {
        id = value;
    }

    @Column(name = "Ma_Nhom", nullable = false, length = 20)
    private String maNhom;

    public String getMaNhom() {
        return maNhom;
    }

    public void setMaNhom(String value) {
        maNhom = value;
    }

    @Column(name = "Ma_SV", nullable = false, length = 20)
    private String maSV;

    public String getMaSV() {
        return maSV;
    }

    public void setMaSV(String value) {
        maSV = value;
    }

    @Column(name = "Trang_Thai", nullable = false, length = 20)
    private String trangThai;

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String value) {
        trangThai = value;
    }

    @Column(name = "Ngay_Moi", nullable = false)
    private LocalDateTime ngayMoi;

    public LocalDateTime getNgayMoi() {
        return ngayMoi;
    }

    public void setNgayMoi(LocalDateTime value) {
        ngayMoi = value;
    }

    @Override
    protected Object entityId() {
        return id;
    }
}
