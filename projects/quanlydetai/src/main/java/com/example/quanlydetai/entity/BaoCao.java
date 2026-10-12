package com.example.quanlydetai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "BAO_CAO")
public class BaoCao extends AbstractEntity {
    @Id
    @Column(name = "Ma_BC", nullable = false, length = 30)
    private String maBc;

    @Column(name = "Ma_Nhom", nullable = false, length = 20)
    private String maNhom;

    @Column(name = "Ma_SV_Nguoi_Nop", nullable = false, length = 20)
    private String maSvNguoiNop;

    @Column(name = "Ngay_Nop", nullable = false)
    private LocalDateTime ngayNop;

    @Column(name = "File_Dinh_Kem", nullable = false, length = 500)
    private String fileDinhKem;

    @Column(name = "Lan_Nop", nullable = false)
    private Integer lanNop;

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
            name = "Ma_SV_Nguoi_Nop",
            referencedColumnName = "Ma_SV",
            insertable = false,
            updatable = false)
    private SinhVien svNguoiNop;

    public SinhVien getSvNguoiNop() {
        return svNguoiNop;
    }

    public String getMaBc() {
        return maBc;
    }

    public void setMaBc(String maBc) {
        this.maBc = maBc;
    }

    public String getMaNhom() {
        return maNhom;
    }

    public void setMaNhom(String maNhom) {
        this.maNhom = maNhom;
    }

    public String getMaSvNguoiNop() {
        return maSvNguoiNop;
    }

    public void setMaSvNguoiNop(String maSvNguoiNop) {
        this.maSvNguoiNop = maSvNguoiNop;
    }

    public LocalDateTime getNgayNop() {
        return ngayNop;
    }

    public void setNgayNop(LocalDateTime ngayNop) {
        this.ngayNop = ngayNop;
    }

    public String getFileDinhKem() {
        return fileDinhKem;
    }

    public void setFileDinhKem(String fileDinhKem) {
        this.fileDinhKem = fileDinhKem;
    }

    public Integer getLanNop() {
        return lanNop;
    }

    public void setLanNop(Integer lanNop) {
        this.lanNop = lanNop;
    }

    @Override
    protected Object entityId() {
        return maBc;
    }
}
