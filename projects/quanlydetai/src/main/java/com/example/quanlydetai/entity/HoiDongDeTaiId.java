package com.example.quanlydetai.entity;

import java.io.Serializable;
import java.util.Objects;

public class HoiDongDeTaiId implements Serializable {
    private static final long serialVersionUID = 1L;
    private String maHd;
    private String maDeTai;

    public HoiDongDeTaiId() {}

    public HoiDongDeTaiId(String maHd, String maDeTai) {
        this.maHd = maHd;
        this.maDeTai = maDeTai;
    }

    public String getMaHd() {
        return maHd;
    }

    public void setMaHd(String maHd) {
        this.maHd = maHd;
    }

    public String getMaDeTai() {
        return maDeTai;
    }

    public void setMaDeTai(String maDeTai) {
        this.maDeTai = maDeTai;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof HoiDongDeTaiId id)) return false;
        return Objects.equals(maHd, id.maHd) && Objects.equals(maDeTai, id.maDeTai);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maHd, maDeTai);
    }
}
