package com.example.quanlydetai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "SV")
public class SinhVien extends AbstractEntity {
    @Id
    @Column(name = "Ma_SV", nullable = false, length = 20)
    private String maSV;

    @Column(name = "Ho_Ten", nullable = false, length = 150)
    private String hoTen;

    @Column(name = "Email", nullable = false, length = 255)
    private String email;

    @Column(name = "SDT", nullable = true, length = 20)
    private String sdt;

    public String getMaSV() {
        return maSV;
    }

    public void setMaSV(String maSV) {
        this.maSV = maSV;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSdt() {
        return sdt;
    }

    public void setSdt(String sdt) {
        this.sdt = sdt;
    }

    @Override
    protected Object entityId() {
        return maSV;
    }
}
