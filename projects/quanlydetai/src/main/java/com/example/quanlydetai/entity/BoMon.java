package com.example.quanlydetai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "BO_MON")
public class BoMon extends AbstractEntity {
    @Id
    @Column(name = "Ma_Bo_Mon", nullable = false, length = 20)
    private String maBoMon;

    @Column(name = "Ten_Bo_Mon", nullable = false, length = 150)
    private String tenBoMon;

    public String getMaBoMon() {
        return maBoMon;
    }

    public void setMaBoMon(String maBoMon) {
        this.maBoMon = maBoMon;
    }

    public String getTenBoMon() {
        return tenBoMon;
    }

    public void setTenBoMon(String tenBoMon) {
        this.tenBoMon = tenBoMon;
    }

    @Override
    protected Object entityId() {
        return maBoMon;
    }
}
