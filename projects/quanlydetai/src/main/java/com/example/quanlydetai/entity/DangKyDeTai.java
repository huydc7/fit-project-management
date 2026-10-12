package com.example.quanlydetai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "DANG_KY_DE_TAI",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"Active_Group"}),
            @UniqueConstraint(columnNames = {"Active_Topic"})
        })
public class DangKyDeTai extends AbstractEntity {
    @Id
    @Column(name = "Ma_DK", nullable = false, length = 30)
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

    @Column(name = "Ma_De_Tai", nullable = false, length = 20)
    private String maDeTai;

    public String getMaDeTai() {
        return maDeTai;
    }

    public void setMaDeTai(String value) {
        maDeTai = value;
    }

    @Column(name = "Trang_Thai", nullable = false, length = 20)
    private String trangThai;

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String value) {
        trangThai = value;
    }

    @Column(name = "Ngay_Dang_Ky", nullable = false)
    private LocalDateTime ngayDangKy;

    public LocalDateTime getNgayDangKy() {
        return ngayDangKy;
    }

    public void setNgayDangKy(LocalDateTime value) {
        ngayDangKy = value;
    }

    @Column(name = "Ly_Do", nullable = true, length = 1000)
    private String lyDo;

    public String getLyDo() {
        return lyDo;
    }

    public void setLyDo(String value) {
        lyDo = value;
    }

    @Column(name = "Ma_GV_Duyet", nullable = true, length = 20)
    private String maGvDuyet;

    public String getMaGvDuyet() {
        return maGvDuyet;
    }

    public void setMaGvDuyet(String value) {
        maGvDuyet = value;
    }

    @Column(name = "Ngay_Duyet", nullable = true)
    private LocalDateTime ngayDuyet;

    public LocalDateTime getNgayDuyet() {
        return ngayDuyet;
    }

    public void setNgayDuyet(LocalDateTime value) {
        ngayDuyet = value;
    }

    @Column(name = "Active_Group", nullable = true, length = 20)
    private String activeGroup;

    public String getActiveGroup() {
        return activeGroup;
    }

    public void setActiveGroup(String value) {
        activeGroup = value;
    }

    @Column(name = "Active_Topic", nullable = true, length = 20)
    private String activeTopic;

    public String getActiveTopic() {
        return activeTopic;
    }

    public void setActiveTopic(String value) {
        activeTopic = value;
    }

    @Override
    protected Object entityId() {
        return id;
    }
}
