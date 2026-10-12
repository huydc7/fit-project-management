package com.example.quanlydetai.entity;

import java.io.Serializable;
import java.util.Objects;

public class ThanhVienHoiDongId implements Serializable {
    private static final long serialVersionUID = 1L;
    private String maHd;
    private String maGV;

    public ThanhVienHoiDongId() {}

    public ThanhVienHoiDongId(String maHd, String maGV) {
        this.maHd = maHd;
        this.maGV = maGV;
    }

    public String getMaHd() {
        return maHd;
    }

    public void setMaHd(String maHd) {
        this.maHd = maHd;
    }

    public String getMaGV() {
        return maGV;
    }

    public void setMaGV(String maGV) {
        this.maGV = maGV;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThanhVienHoiDongId id)) return false;
        return Objects.equals(maHd, id.maHd) && Objects.equals(maGV, id.maGV);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maHd, maGV);
    }
}
