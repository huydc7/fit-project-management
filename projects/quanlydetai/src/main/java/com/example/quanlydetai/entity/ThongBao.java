package com.example.quanlydetai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "THONG_BAO")
public class ThongBao extends AbstractEntity {
    @Id
    @Column(name = "Ma_TB", nullable = false, length = 30)
    private String maTb;

    @Column(name = "Tieu_De", nullable = false, length = 300)
    private String tieuDe;

    @Column(name = "Noi_Dung", nullable = false, columnDefinition = "TEXT")
    private String noiDung;

    @Column(name = "Ngay_Dang", nullable = false)
    private LocalDateTime ngayDang;

    @Column(name = "Pham_Vi", nullable = false, length = 20)
    private String phamVi;

    @Column(name = "Ma_TK", nullable = false, length = 30)
    private String maTk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "Ma_TK",
            referencedColumnName = "Ma_TK",
            insertable = false,
            updatable = false)
    private TaiKhoan tk;

    public TaiKhoan getTk() {
        return tk;
    }

    public String getMaTb() {
        return maTb;
    }

    public void setMaTb(String maTb) {
        this.maTb = maTb;
    }

    public String getTieuDe() {
        return tieuDe;
    }

    public void setTieuDe(String tieuDe) {
        this.tieuDe = tieuDe;
    }

    public String getNoiDung() {
        return noiDung;
    }

    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }

    public LocalDateTime getNgayDang() {
        return ngayDang;
    }

    public void setNgayDang(LocalDateTime ngayDang) {
        this.ngayDang = ngayDang;
    }

    public String getPhamVi() {
        return phamVi;
    }

    public void setPhamVi(String phamVi) {
        this.phamVi = phamVi;
    }

    public String getMaTk() {
        return maTk;
    }

    public void setMaTk(String maTk) {
        this.maTk = maTk;
    }

    @Override
    protected Object entityId() {
        return maTb;
    }
}
