package com.example.quanlydetai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "LOAI_DIEM")
public class LoaiDiem extends AbstractEntity {
    @Id
    @Column(name = "Ma_Loai_Diem", nullable = false, length = 20)
    private String maLoaiDiem;

    @Column(name = "Ten_Loai_Diem", nullable = false, length = 150)
    private String tenLoaiDiem;

    public String getMaLoaiDiem() {
        return maLoaiDiem;
    }

    public void setMaLoaiDiem(String maLoaiDiem) {
        this.maLoaiDiem = maLoaiDiem;
    }

    public String getTenLoaiDiem() {
        return tenLoaiDiem;
    }

    public void setTenLoaiDiem(String tenLoaiDiem) {
        this.tenLoaiDiem = tenLoaiDiem;
    }

    @Override
    protected Object entityId() {
        return maLoaiDiem;
    }
}
