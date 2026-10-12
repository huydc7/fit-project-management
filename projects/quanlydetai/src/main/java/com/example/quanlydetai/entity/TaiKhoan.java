package com.example.quanlydetai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TAI_KHOAN")
public class TaiKhoan extends AbstractEntity {
    @Id
    @Column(name = "Ma_TK", nullable = false, length = 30)
    private String maTk;

    @Column(name = "Ma_SV", nullable = true, length = 20)
    private String maSV;

    @Column(name = "Ma_GV", nullable = true, length = 20)
    private String maGV;

    @Column(name = "Ten_Dang_Nhap", nullable = false, length = 100)
    private String tenDangNhap;

    @Column(name = "Mat_Khau_Hash", nullable = false, length = 255)
    private String matKhauHash;

    @Column(name = "Vai_Tro", nullable = false, length = 20)
    private String vaiTro;

    @Column(name = "Trang_Thai", nullable = false, length = 20)
    private String trangThai;

    @Column(name = "Ngay_Tao", nullable = false)
    private LocalDateTime ngayTao;

    @Column(name = "Lan_Dang_Nhap_CuoiCung", nullable = true)
    private LocalDateTime lanDangNhapCuoicung;

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

    public String getMaTk() {
        return maTk;
    }

    public void setMaTk(String maTk) {
        this.maTk = maTk;
    }

    public String getMaSV() {
        return maSV;
    }

    public void setMaSV(String maSV) {
        this.maSV = maSV;
    }

    public String getMaGV() {
        return maGV;
    }

    public void setMaGV(String maGV) {
        this.maGV = maGV;
    }

    public String getTenDangNhap() {
        return tenDangNhap;
    }

    public void setTenDangNhap(String tenDangNhap) {
        this.tenDangNhap = tenDangNhap;
    }

    public String getMatKhauHash() {
        return matKhauHash;
    }

    public void setMatKhauHash(String matKhauHash) {
        this.matKhauHash = matKhauHash;
    }

    public String getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(String vaiTro) {
        this.vaiTro = vaiTro;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }

    public LocalDateTime getLanDangNhapCuoicung() {
        return lanDangNhapCuoicung;
    }

    public void setLanDangNhapCuoicung(LocalDateTime lanDangNhapCuoicung) {
        this.lanDangNhapCuoicung = lanDangNhapCuoicung;
    }

    @Override
    protected Object entityId() {
        return maTk;
    }
}
