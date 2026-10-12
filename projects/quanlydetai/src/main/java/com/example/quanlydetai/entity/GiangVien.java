package com.example.quanlydetai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "GV")
public class GiangVien extends AbstractEntity {
    @Id
    @Column(name = "Ma_GV", nullable = false, length = 20)
    private String maGV;

    @Column(name = "Ho_Ten", nullable = false, length = 150)
    private String hoTen;

    @Column(name = "Email", nullable = false, length = 255)
    private String email;

    @Column(name = "SDT", nullable = true, length = 20)
    private String sdt;

    @Column(name = "Ma_Bo_Mon", nullable = false, length = 20)
    private String maBoMon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "Ma_Bo_Mon",
            referencedColumnName = "Ma_Bo_Mon",
            insertable = false,
            updatable = false)
    private BoMon boMon;

    public BoMon getBoMon() {
        return boMon;
    }

    public String getMaGV() {
        return maGV;
    }

    public void setMaGV(String maGV) {
        this.maGV = maGV;
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

    public String getMaBoMon() {
        return maBoMon;
    }

    public void setMaBoMon(String maBoMon) {
        this.maBoMon = maBoMon;
    }

    @Override
    protected Object entityId() {
        return maGV;
    }
}
